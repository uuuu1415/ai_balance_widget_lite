package com.example.balancewidget

import android.os.Bundle
import android.content.Intent
import android.app.DownloadManager
import android.graphics.Color
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Environment
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.activity.OnBackPressedCallback
import com.example.balancewidget.data.Account
import com.example.balancewidget.data.AccountStore
import com.example.balancewidget.data.BalanceClient
import com.example.balancewidget.data.BalanceSnapshot
import com.example.balancewidget.data.SnapshotStore
import com.example.balancewidget.widget.WidgetUpdater
import com.example.balancewidget.widget.WidgetRefreshScheduler
import com.example.balancewidget.settings.AppSettings
import com.example.balancewidget.settings.UpdateChecker
import com.example.balancewidget.settings.ReleaseInfo
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var store: AccountStore
    private lateinit var snapshots: SnapshotStore
    private val client = BalanceClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var accountList: LinearLayout
    private var showingEditor = false
    private lateinit var settings: AppSettings

    override fun onCreate(state: Bundle?) {
        settings = AppSettings(this)
        if (settings.dynamicColorEnabled) DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(state)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        store = AccountStore(this)
        snapshots = SnapshotStore(this)
        WidgetRefreshScheduler.apply(this)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (showingEditor) showAccounts() else finish()
            }
        })
        showAccounts()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun showAccounts() {
        showingEditor = false
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val toolbar = toolbar("余额小组件", "本地保存的 AI 平台余额").apply { addSettingsAction() }
        root.addView(toolbar, matchWrap())

        val scroll = ScrollView(this).apply { isFillViewport = true }
        val content = contentColumn()
        content.addView(TextView(this).apply {
            text = "账户"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
        }, matchWrap(bottom = 4))
        content.addView(TextView(this).apply {
            text = "余额仅保存在此设备。点击刷新会立即查询对应接口。"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
        }, matchWrap(bottom = 20))
        accountList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(accountList, matchWrap())
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val bottomBar = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(12), dp(24), dp(12))
        }
        bottomBar.addView(MaterialButton(this).apply {
            text = "添加账户"
            setIconResource(android.R.drawable.ic_input_add)
            setOnClickListener { showEditor(null) }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { width = minOf(dp(640), resources.displayMetrics.widthPixels - dp(48)) })
        root.addView(bottomBar, matchWrap())
        setContentView(root)
        applyCustomColor(root)
        applyInsets(root)
        ViewCompat.requestApplyInsets(root)
        renderAccounts()
    }

    private fun renderAccounts() {
        accountList.removeAllViews()
        val accounts = store.accounts()
        if (accounts.isEmpty()) {
            accountList.addView(emptyState("还没有账户", "添加 DeepSeek、SevnX 或自定义中转站账户后，即可在主屏幕添加 Widget。"))
            return
        }
        accounts.forEach { account -> accountList.addView(accountCard(account), matchWrap(bottom = 12)) }
    }

    private fun accountCard(account: Account): View {
        val snapshot = snapshots.get(account.id)
        return MaterialCardView(this).apply {
            isClickable = true
            isFocusable = true
            strokeWidth = dp(1)
            val inner = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(18), dp(20), dp(12))
            }
            inner.addView(TextView(context).apply {
                text = account.name
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
            }, matchWrap(bottom = 4))
            inner.addView(TextView(context).apply {
                text = format(snapshot)
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            }, matchWrap(bottom = 12))
            val actions = LinearLayout(context).apply { gravity = Gravity.END }
            actions.addView(textButton("刷新") { refresh(account) })
            actions.addView(textButton("编辑") { showEditor(account) })
            actions.addView(textButton("删除") { store.delete(account.id); renderAccounts(); WidgetUpdater.update(this@MainActivity) })
            inner.addView(actions, matchWrap())
            addView(inner)
        }
    }

    private fun showEditor(existing: Account?) {
        showingEditor = true
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val toolbar = toolbar(if (existing == null) "添加账户" else "编辑账户", "配置余额接口和 JSON 字段路径").apply {
            setNavigationIcon(com.google.android.material.R.drawable.ic_arrow_back_black_24)
            setNavigationOnClickListener { showAccounts() }
        }
        root.addView(toolbar, matchWrap())
        val scroll = ScrollView(this)
        val form = contentColumn()
        form.addView(TextView(this).apply {
            text = "接口配置"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
        }, matchWrap(bottom = 12))
        val name = input(form, "名称", "例如：DeepSeek 个人账号", existing?.name.orEmpty())
        val provider = input(form, "Provider 类型", "custom、deepseek 或 relay（中转站）", existing?.provider ?: "custom")
        val base = input(form, "Base URL", "例如：https://api.deepseek.com", existing?.baseUrl.orEmpty(), InputType.TYPE_TEXT_VARIATION_URI)
        val path = input(form, "余额 API 路径", "例如：user/balance 或 v1/usage", existing?.path ?: "v1/usage")
        val key = input(form, "API Key", "仅保存于本机，未加密", existing?.apiKey.orEmpty(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        form.addView(TextView(this).apply {
            text = "JSON 字段映射"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
        }, matchWrap(top = 16, bottom = 8))
        val balance = input(form, "余额字段路径", "例如：balance 或 balance_infos[0].total_balance", existing?.balancePath ?: "balance")
        val currency = input(form, "币种字段路径（可选）", "例如：unit 或 balance_infos[0].currency", existing?.currencyPath ?: "unit")
        val available = input(form, "可用状态字段路径（可选）", "例如：isValid", existing?.availablePath ?: "isValid")
        val today = input(form, "今日消费字段路径（可选）", "例如：usage.today.actual_cost", existing?.todayCostPath ?: "usage.today.actual_cost")
        val total = input(form, "累计消费字段路径（可选）", "例如：usage.total.actual_cost", existing?.totalCostPath ?: "usage.total.actual_cost")
        form.addView(TextView(this).apply {
            text = "路径支持点号与数组下标，如 usage.today.actual_cost、data[0].balance。DeepSeek 类型会自动使用 /user/balance。"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
        }, matchWrap(top = 4, bottom = 16))
        scroll.addView(form)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        val actions = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(dp(24), dp(12), dp(24), dp(12)) }
        actions.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "测试连接"
            setOnClickListener { test(toAccount(existing, name, provider, base, path, key, balance, currency, available, today, total)) }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) })
        actions.addView(MaterialButton(this).apply {
            text = "保存"
            setOnClickListener {
                store.save(toAccount(existing, name, provider, base, path, key, balance, currency, available, today, total))
                WidgetUpdater.update(this@MainActivity)
                showAccounts()
            }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(actions, matchWrap())
        setContentView(root)
        applyCustomColor(root)
        applyInsets(root)
        ViewCompat.requestApplyInsets(root)
    }

    private fun showSettings() {
        showingEditor = true
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val toolbar = toolbar("设置", "个性化应用与更新").apply {
            setNavigationIcon(com.google.android.material.R.drawable.ic_arrow_back_black_24)
            setNavigationOnClickListener { showAccounts() }
        }
        root.addView(toolbar, matchWrap())

        val scroll = ScrollView(this).apply { isFillViewport = true }
        val content = contentColumn()
        content.addView(sectionTitle("外观"), matchWrap(bottom = 12))
        val dynamic = SwitchMaterial(this).apply {
            text = "使用系统动态颜色"
            isChecked = settings.dynamicColorEnabled
            setOnCheckedChangeListener { _, enabled ->
                settings.dynamicColorEnabled = enabled
                recreate()
            }
        }
        content.addView(dynamic, matchWrap(bottom = 8))

        val colorCard = MaterialCardView(this).apply {
            val row = LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(20), dp(14), dp(20), dp(14))
                addView(TextView(context).apply {
                    text = "应用主题色\n${String.format("#%06X", settings.customColor and 0xFFFFFF)}"
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = "更改"
                    setOnClickListener { showColorDialog() }
                })
            }
            addView(row)
        }
        content.addView(colorCard, matchWrap(bottom = 8))
        content.addView(TextView(this).apply {
            text = "关闭动态颜色后，应用会使用你选择的主题色。更改主题色后会立即刷新界面。"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
        }, matchWrap(bottom = 20))

        content.addView(sectionTitle("更新"), matchWrap(bottom = 8))
        content.addView(MaterialCardView(this).apply {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(14), dp(20), dp(14))
                addView(TextView(context).apply {
                    text = if (settings.refreshIntervalMinutes == 0) "自动刷新：已关闭" else "自动刷新间隔：${settings.refreshIntervalMinutes} 分钟"
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                }, matchWrap(bottom = 8))
                addView(MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = "更改刷新间隔"
                    setOnClickListener { chooseRefreshInterval() }
                }, matchWrap())
            }
            addView(row)
        }, matchWrap(bottom = 8))
        content.addView(TextView(this).apply {
            text = "可输入任意分钟数。Android 会根据省电策略调整后台更新时间；主屏幕刷新按钮始终可以手动查询。"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
        }, matchWrap(bottom = 12))
        content.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "检查更新"
            setOnClickListener { checkForUpdates() }
        }, matchWrap(bottom = 8))
        content.addView(MaterialButton(this).apply {
            text = "查看发布记录"
            setOnClickListener { openUrl(UpdateChecker.RELEASES_URL) }
        }, matchWrap(bottom = 20))

        content.addView(sectionTitle("关于"), matchWrap(bottom = 8))
        content.addView(MaterialCardView(this).apply {
            val about = TextView(context).apply {
                text = "余额小组件\n作者：uuuu1415\n版本：${BuildConfig.VERSION_NAME}\n\n本项目使用 Vibe Coding 方式开发，主要使用 GPT-5.6 Terra 协助完成。\n\n本软件依据 MIT License 开源。你可以自由使用、修改和分发，但须保留原作者版权声明和许可证文本。"
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                setPadding(dp(20), dp(18), dp(20), dp(18))
            }
            addView(about)
        }, matchWrap())
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        applyCustomColor(root)
        applyInsets(root)
        ViewCompat.requestApplyInsets(root)
    }

    private fun showColorDialog() {
        val input = TextInputEditText(this).apply {
            hint = "#405F91"
            setText(String.format("#%06X", settings.customColor and 0xFFFFFF))
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
        }
        val palette = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        val colors = intArrayOf(
            Color.parseColor("#405F91"),
            Color.parseColor("#006C4C"),
            Color.parseColor("#8B4E00"),
            Color.parseColor("#9C4146"),
            Color.parseColor("#65558F"),
            Color.parseColor("#00658A")
        )
        colors.forEach { color ->
            palette.addView(MaterialButton(this).apply {
                text = ""
                contentDescription = String.format("选择主题色 #%06X", color and 0xFFFFFF)
                backgroundTintList = ColorStateList.valueOf(color)
                minWidth = 0
                minimumWidth = dp(40)
                minHeight = 0
                minimumHeight = dp(40)
                setOnClickListener { input.setText(String.format("#%06X", color and 0xFFFFFF)) }
            }, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(4) })
        }
        val field = TextInputLayout(this).apply { hint = "主题色 HEX"; addView(input) }
        content.addView(TextView(this).apply { text = "调色盘"; setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge) }, matchWrap(bottom = 8))
        content.addView(palette, matchWrap(bottom = 16))
        content.addView(field, matchWrap())
        MaterialAlertDialogBuilder(this)
            .setTitle("更改主题色")
            .setView(content)
            .setMessage("请输入 #RRGGBB 或 #AARRGGBB，例如 #405F91")
            .setNegativeButton("取消", null)
            .setPositiveButton("应用") { _, _ ->
                runCatching { Color.parseColor(input.text.toString().trim()) }
                    .onSuccess { settings.customColor = it; recreate() }
                    .onFailure { Toast.makeText(this, "颜色格式无效", Toast.LENGTH_SHORT).show() }
            }.show()
    }

    private fun chooseRefreshInterval() {
        val input = TextInputEditText(this).apply {
            hint = "例如：30；输入 0 关闭自动刷新"
            setText(settings.refreshIntervalMinutes.toString())
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val field = TextInputLayout(this).apply { hint = "刷新间隔（分钟）"; addView(input) }
        MaterialAlertDialogBuilder(this).setTitle("设置自动刷新间隔")
            .setView(field)
            .setMessage("可输入任意非负整数分钟数。系统会使用不精确定时任务，实际执行时间可能延后。")
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val minutes = input.text.toString().toIntOrNull()
                if (minutes == null || minutes < 0) {
                    Toast.makeText(this, "请输入非负整数", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                settings.refreshIntervalMinutes = minutes
                WidgetRefreshScheduler.apply(this)
                showSettings()
            }.show()
    }

    private fun checkForUpdates() {
        Toast.makeText(this, "正在检查更新", Toast.LENGTH_SHORT).show()
        scope.launch {
            UpdateChecker().latestRelease().onSuccess { release ->
                val current = BuildConfig.VERSION_NAME
                if (release.version.isNotBlank() && UpdateChecker.isNewer(release.version, current)) {
                    showUpdatePage(release)
                } else Toast.makeText(this@MainActivity, "当前已是最新版本", Toast.LENGTH_LONG).show()
            }.onFailure { error -> Toast.makeText(this@MainActivity, "检查失败：${error.message}", Toast.LENGTH_LONG).show() }
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun showUpdatePage(release: ReleaseInfo) {
        showingEditor = true
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(toolbar("发现新版本", "${BuildConfig.VERSION_NAME} -> ${release.version}").apply {
            setNavigationIcon(com.google.android.material.R.drawable.ic_arrow_back_black_24)
            setNavigationOnClickListener { showSettings() }
        }, matchWrap())
        val scroll = ScrollView(this)
        val content = contentColumn()
        content.addView(sectionTitle("BalanceWidget ${release.version}"), matchWrap(bottom = 8))
        content.addView(TextView(this).apply {
            text = if (release.notes.isBlank()) "此版本未提供更新说明。" else release.notes
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
        }, matchWrap(bottom = 24))
        if (release.apkUrl != null) {
            content.addView(MaterialButton(this).apply {
                text = "下载并更新 APK"
                setIconResource(android.R.drawable.stat_sys_download)
                setOnClickListener { downloadUpdate(release) }
            }, matchWrap())
        } else {
            content.addView(emptyState("尚无 APK 下载", "此 Release 没有上传 APK 文件。可以在发布记录中查看详情。"), matchWrap(bottom = 12))
            content.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "查看发布记录"
                setOnClickListener { openUrl(release.pageUrl) }
            }, matchWrap())
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        applyCustomColor(root)
        applyInsets(root)
        ViewCompat.requestApplyInsets(root)
    }

    private fun downloadUpdate(release: ReleaseInfo) {
        val apkUrl = release.apkUrl ?: return
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("BalanceWidget ${release.version}")
            .setDescription("正在下载更新安装包")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "BalanceWidget-${release.version}.apk")
        getSystemService(DownloadManager::class.java).enqueue(request)
        Toast.makeText(this, "已开始下载，完成后请从通知栏安装", Toast.LENGTH_LONG).show()
    }

    private fun sectionTitle(text: String) = TextView(this).apply {
        this.text = text
        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
    }

    /** Applies the user-selected accent without replacing the Material surface hierarchy. */
    private fun applyCustomColor(root: View) {
        if (settings.dynamicColorEnabled) return
        val accent = ColorStateList.valueOf(settings.customColor)
        root.setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurface))
        fun visit(view: View) {
            if (view is MaterialButton) view.backgroundTintList = accent
            if (view is MaterialToolbar) view.setBackgroundColor(settings.customColor)
            if (view is ViewGroup) for (index in 0 until view.childCount) visit(view.getChildAt(index))
        }
        visit(root)
    }

    private fun resolveColor(attribute: Int): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(attribute, typedValue, true)
        return typedValue.data
    }

    private fun refresh(account: Account) {
        Toast.makeText(this, "正在查询 ${account.name}", Toast.LENGTH_SHORT).show()
        scope.launch {
            val result = client.fetch(account)
            snapshots.save(result)
            renderAccounts()
            WidgetUpdater.update(this@MainActivity)
            Toast.makeText(this@MainActivity, result.error ?: "更新成功", Toast.LENGTH_LONG).show()
        }
    }

    private fun test(account: Account) {
        scope.launch {
            val result = client.fetch(account)
            val message = result.error ?: "连接成功：${result.balance ?: "未解析到余额"} ${result.currency.orEmpty()}"
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun toolbar(title: String, subtitle: String) = MaterialToolbar(this).apply {
        this.title = title
        this.subtitle = subtitle
        setTitleTextAppearance(this@MainActivity, com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
        setSubtitleTextAppearance(this@MainActivity, com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
        setPadding(dp(8), dp(8), dp(8), dp(8))
    }

    private fun MaterialToolbar.addSettingsAction() {
        menu.add("设置").apply {
            setIcon(android.R.drawable.ic_menu_preferences)
            setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        setOnMenuItemClickListener { showSettings(); true }
    }

    private fun input(parent: LinearLayout, label: String, placeholder: String, value: String, type: Int = InputType.TYPE_CLASS_TEXT): TextInputEditText {
        val layout = TextInputLayout(this).apply {
            hint = label
            placeholderText = placeholder
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }
        val edit = TextInputEditText(layout.context).apply { setText(value); inputType = type; maxLines = 1 }
        layout.addView(edit)
        parent.addView(layout, matchWrap(bottom = 12))
        return edit
    }

    private fun contentColumn() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(24), dp(24), dp(24), dp(24))
        val maxWidth = minOf(dp(720), resources.displayMetrics.widthPixels)
        layoutParams = FrameLayout.LayoutParams(maxWidth, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL)
    }

    private fun emptyState(title: String, body: String) = MaterialCardView(this).apply {
        val inner = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(24), dp(24), dp(24)) }
        inner.addView(TextView(context).apply { text = title; setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium) }, matchWrap(bottom = 6))
        inner.addView(TextView(context).apply { text = body; setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium) })
        addView(inner)
    }

    private fun textButton(label: String, action: () -> Unit) = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply { text = label; setOnClickListener { action() } }

    private fun toAccount(old: Account?, name: TextInputEditText, provider: TextInputEditText, base: TextInputEditText, path: TextInputEditText, key: TextInputEditText, balance: TextInputEditText, currency: TextInputEditText, available: TextInputEditText, today: TextInputEditText, total: TextInputEditText) = Account(old?.id ?: AccountStore.newId(), name.text.toString().trim(), base.text.toString().trim(), path.text.toString().trim(), key.text.toString(), provider.text.toString().trim().lowercase(), balance.text.toString().trim(), currency.text.toString().trim(), available.text.toString().trim(), today.text.toString().trim(), total.text.toString().trim())
    private fun format(s: BalanceSnapshot?) = if (s == null) "尚未查询" else if (s.error != null) "${s.balance ?: "暂无余额"} ${s.currency.orEmpty()} · 更新失败" else "${s.balance ?: "-"} ${s.currency.orEmpty()} · ${java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(s.fetchedAt)}"
    /** Applies the system bars to the page root so every replaced page remains inset-safe. */
    private fun applyInsets(root: View) {
        val baseTop = root.paddingTop
        val baseBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = baseTop + systemBars.top,
                bottom = baseBottom + systemBars.bottom
            )
            insets
        }
    }
    private fun matchWrap(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top); bottomMargin = dp(bottom) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
