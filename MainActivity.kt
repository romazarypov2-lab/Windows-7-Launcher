package com.winexelauncher.app
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.*
import android.media.*
import android.os.Bundle
import android.view.*
import android.view.animation.OvershootInterpolator
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File

data class LauncherApp(val title: String, val icon: Drawable, val packName: String)

class MainActivity : AppCompatActivity() {
    private lateinit var startMenu: LinearLayout
    private lateinit var desktop: RelativeLayout
    private lateinit var taskbar: LinearLayout
    private lateinit var diskEPath: File
    private lateinit var trashPath: File
    private lateinit var backupPath: File
    private val exeEngine = ExeEngine()
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        
        Thread { try { mediaPlayer = MediaPlayer().apply { setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build()); setDataSource("https://soundboard.com"); prepare(); start() } } catch (e: Exception) {} }.start()

        diskEPath = File(filesDir, "Disk_E").apply { if (!exists()) mkdirs() }
        trashPath = File(filesDir, "Recycle_Bin").apply { if (!exists()) mkdirs() }
        backupPath = File(filesDir, "System_Restore_Points").apply { if (!exists()) mkdirs() }

        desktop = RelativeLayout(this).apply { backgroundColor = Color.parseColor(prefs.getString("wallpaper_color", "#1d78d6")) }

        taskbar = LinearLayout(this).apply { id = View.generateViewId(); orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; background = GradientDrawable().apply { setColor(Color.parseColor(prefs.getString("taskbar_color", "#CC223344"))) }; setPadding(20, 0, 40, 0) }
        val btnStart = ImageView(this).apply { setImageDrawable(packageManager.getApplicationIcon(packageName)); background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#44FFFFFF")) }; setPadding(10, 10, 10, 10) }
        taskbar.addView(btnStart, LinearLayout.LayoutParams(100, 100))
        taskbar.addView(View(this), LinearLayout.LayoutParams(0, 10).apply { weight = 1f })
        taskbar.addView(TextClock(this).apply { format24Hour = "HH:mm"; setTextColor(Color.WHITE); textSize = 14f; setTypeface(null, Typeface.BOLD) })
        desktop.addView(taskbar, RelativeLayout.LayoutParams(-1, 130).apply { addRule(RelativeLayout.ALIGN_PARENT_BOTTOM) })

        startMenu = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; backgroundColor = Color.parseColor("#F0F0F0"); visibility = View.GONE; elevation = 30f; setPadding(30, 20, 30, 20) }
        startMenu.addView(TextView(this).apply { text = "Панель управления"; setTextColor(Color.BLACK); setTypeface(null, Typeface.BOLD); setPadding(0, 0, 0, 10) })
        startMenu.addView(Button(this).apply { text = "🎮 Диск E: (Игры)"; setOnClickListener { animateMenu(false); openGameExplorer(diskEPath) } })
        
        val spinWall = Spinner(this).apply { adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, arrayOf("Синий", "Черный", "Серый")); setSelection(prefs.getInt("w_p", 0)) }
        spinWall.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val c = when(pos) { 0 -> "#1d78d6"; 1 -> "#000000"; else -> "#D3D3D3" }
                desktop.backgroundColor = Color.parseColor(c)
                prefs.edit().putString("wallpaper_color", c).putInt("w_p", pos).apply()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        startMenu.addView(spinWall)
        startMenu.addView(CheckBox(this).apply { text = "Автозапуск"; isChecked = prefs.getBoolean("a_k", false); setOnCheckedChangeListener { _, isC -> prefs.edit().putBoolean("a_k", isC).apply() } })
        startMenu.addView(Button(this).apply { text = "🔴 Выключение"; setTextColor(Color.WHITE); setBackgroundColor(Color.RED); setOnClickListener { finishAffinity() } })
        desktop.addView(startMenu, RelativeLayout.LayoutParams(700, 950).apply { addRule(RelativeLayout.ABOVE, taskbar.id) })

        btnStart.setOnClickListener { if (startMenu.visibility == View.VISIBLE) animateMenu(false) else animateMenu(true) }
    private fun animateMenu(show: Boolean) {
        if (show) {
            startMenu.visibility = View.VISIBLE
            startMenu.translationY = startMenu.height.toFloat()
            startMenu.alpha = 0f
            startMenu.animate().translationY(0f).alpha(1f).setDuration(350).setInterpolator(OvershootInterpolator()).start()
        } else {
            startMenu.animate().translationY(startMenu.height.toFloat()).alpha(0f).setDuration(250).withEndAction { startMenu.visibility = View.GONE }.start()
        }
    }

    private fun animateWindowIn(window: View) {
        window.scaleX = 0.7f
        window.scaleY = 0.7f
        window.alpha = 0f
        val scaleX = ObjectAnimator.ofFloat(window, View.SCALE_X, 1f)
        val scaleY = ObjectAnimator.ofFloat(window, View.SCALE_Y, 1f)
        val alpha = ObjectAnimator.ofFloat(window, View.ALPHA, 1f)
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 400
            interpolator = OvershootInterpolator(1.2f)
            start()
        }
    }

    private fun openGoogleChrome() {
        val win = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; backgroundColor = Color.parseColor("#EAEAEA"); elevation = 50f; setPadding(10, 10, 10, 10) }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(TextView(this).apply { text = "🌐 Google Chrome"; setTextColor(Color.BLACK); setTypeface(null, Typeface.BOLD) }, LinearLayout.LayoutParams(0, -2).apply { weight = 1f })
        val btnC = Button(this).apply { text = "X"; setBackgroundColor(Color.RED); setTextColor(Color.WHITE); setOnClickListener { desktop.removeView(win) } }
        head.addView(btnC, LinearLayout.LayoutParams(100, 80)); win.addView(head)
        
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val et = EditText(this).apply { setText("https://google.com"); textSize = 14f; maxLines = 1; setBackgroundColor(Color.WHITE); setTextColor(Color.BLACK) }
        val btnG = Button(this).apply { text = "Go" }
        bar.addView(et, LinearLayout.LayoutParams(0, -2).apply { weight = 1f })
        bar.addView(btnG, LinearLayout.LayoutParams(150, 90)); win.addView(bar)
        
        val web = WebView(this).apply { 
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()
            setDownloadListener { url, _, contentDisposition, mimeType, _ ->
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val targetFile = File(File(diskEPath, "Загрузки").apply { if (!exists()) mkdirs() }, fileName)
                Toast.makeText(context, "Скачивание на Диск E: $fileName", Toast.LENGTH_LONG).show()
                Thread { try { java.net.URL(url).openStream().use { input -> targetFile.outputStream().use { input.copyTo(it) } }; runOnUiThread { Toast.makeText(context, "Успешно сохранено на Диск E!", Toast.LENGTH_SHORT).show() } } catch(e: Exception) {} }.start()
            }
            loadUrl("https://google.com")
        }
        win.addView(web, LinearLayout.LayoutParams(-1, 0).apply { weight = 1f })
        btnG.setOnClickListener { var u = et.text.toString(); if(!u.startsWith("http")) u = "https://$u"; web.loadUrl(u) }
        
        desktop.addView(win, RelativeLayout.LayoutParams(1000, 1500).apply { addRule(RelativeLayout.CENTER_IN_PARENT) })
        animateWindowIn(win)
    }
        private fun openComputerWindow() {
        val win = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; backgroundColor = Color.WHITE; elevation = 40f; setPadding(20, 20, 20, 20) }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        head.addView(TextView(this).apply { text = "📁 Мой Компьютер"; setTextColor(Color.BLACK); setTypeface(null, Typeface.BOLD) }, LinearLayout.LayoutParams(0, -2).apply { weight = 1f })
        val btnC = Button(this).apply { text = "X"; setBackgroundColor(Color.RED); setTextColor(Color.WHITE); setOnClickListener { desktop.removeView(win) } }
        head.addView(btnC, LinearLayout.LayoutParams(100, 80)); win.addView(head)
        
        val lv = ListView(this)
        val items = arrayOf("💽 Локальный диск (E:)", "🗑️ Корзина (" + (trashPath.listFiles()?.size ?: 0) + " файлов)")
        lv.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        lv.setOnItemClickListener { _, _, pos, _ -> desktop.removeView(win); if (pos == 0) openGameExplorer(diskEPath) else openTrashExplorer() }
        win.addView(lv, LinearLayout.LayoutParams(-1, 0).apply { weight = 1f })
        desktop.addView(win, RelativeLayout.LayoutParams(950, 1300).apply { addRule(RelativeLayout.CENTER_IN_PARENT) })
        animateWindowIn(win)
    }

    private fun openTrashExplorer() {
        val win = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; backgroundColor = Color.WHITE; elevation = 40f; setPadding(20, 20, 20, 20) }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        head.addView(TextView(this).apply { text = "🗑️ Корзина Windows"; setTextColor(Color.BLACK); setTypeface(null, Typeface.BOLD) }, LinearLayout.LayoutParams(0, -2).apply { weight = 1f })
        val btnClear = Button(this).apply { text = "Очистить"; setBackgroundColor(Color.DKGRAY); setTextColor(Color.WHITE) }
        val btnC = Button(this).apply { text = "X"; setBackgroundColor(Color.RED); setTextColor(Color.WHITE); setOnClickListener { desktop.removeView(win) } }
        head.addView(btnClear); head.addView(btnC, LinearLayout.LayoutParams(100, 80)); win.addView(head)
        
        val lv = ListView(this)
        val trashFiles = trashPath.listFiles()?.map { it.name } ?: listOf()
        lv.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, trashFiles)
        lv.setOnItemClickListener { _, _, pos, _ ->
            val fileToRestore = trashPath.listFiles()?.get(pos)
            if (fileToRestore != null) {
                fileToRestore.renameTo(File(diskEPath, fileToRestore.name))
                Toast.makeText(this, "Файл восстановлен на Диск E", Toast.LENGTH_SHORT).show()
                desktop.removeView(win); openTrashExplorer()
            }
        }
        btnClear.setOnClickListener { trashPath.listFiles()?.forEach { it.delete() }; desktop.removeView(win); openTrashExplorer(); Toast.makeText(this, "Корзина очищена", Toast.LENGTH_SHORT).show() }
        win.addView(lv, LinearLayout.LayoutParams(-1, 0).apply { weight = 1f })
        desktop.addView(win, RelativeLayout.LayoutParams(950, 1300).apply { addRule(RelativeLayout.CENTER_IN_PARENT) })
        animateWindowIn(win)
    }

    private fun openRestoreWindow() {
        val win = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; backgroundColor = Color.WHITE; elevation = 45f; setPadding(20, 20, 20, 20) }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        head.addView(TextView(this).apply { text = "🛡️ Восстановление системы"; setTextColor(Color.BLACK); setTypeface(null, Typeface.BOLD) }, LinearLayout.LayoutParams(0, -2).apply { weight = 1f })
        val btnC = Button(this).apply { text = "X"; setBackgroundColor(Color.RED); setTextColor(Color.WHITE); setOnClickListener { desktop.removeView(win) } }
        head.addView(btnC, LinearLayout.LayoutParams(100, 80)); win.addView(head)

        val btnCreate = Button(this).apply { text = "📸 Создать точку сохранения"; setOnClickListener {
            diskEPath.copyRecursively(File(backupPath, "Disk_E_Backup"), overwrite = true)
            Toast.makeText(context, "Точка сохранения успешно создана!", Toast.LENGTH_SHORT).show()
        }}
        val btnLoad = Button(this).apply { text = "↩️ Откатить систему назад"; setOnClickListener {
            val backupFolder = File(backupPath, "Disk_E_Backup")
            if (backupFolder.exists()) {
                backupFolder.copyRecursively(diskEPath, overwrite = true)
                Toast.makeText(context, "Система успешно восстановлена!", Toast.LENGTH_SHORT).show()
                desktop.removeView(win)
            } else { Toast.makeText(context, "Сохранения не найдены!", Toast.LENGTH_SHORT).show() }
        }}
        win.addView(btnCreate); win.addView(btnLoad)
        desktop.addView(win, RelativeLayout.LayoutParams(900, 800).apply { addRule(RelativeLayout.CENTER_IN_PARENT) })
        animateWindowIn(win)
    }

    private fun openGameExplorer(currentDir: File) {
        val win = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; backgroundColor = Color.WHITE; elevation = 40f; setPadding(20, 20, 20, 20) }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        head.addView(TextView(this).apply { text = "Диск E:\\ " + currentDir.name; setTextColor(Color.BLACK); setTypeface(null, Typeface.BOLD) }, LinearLayout.LayoutParams(0, -2).apply { weight = 1f })
        val btnC = Button(this).apply { text = "X"; setBackgroundColor(Color.RED); setTextColor(Color.WHITE); setOnClickListener { desktop.removeView(win) } }
        head.addView(btnC, LinearLayout.LayoutParams(100, 80)); win.addView(head)
        
        val lv = ListView(this)
        val files = currentDir.listFiles()?.map { if (it.isDirectory) "📁 " + it.name else "🎮 " + it.name } ?: listOf()
        lv.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, files)
        
        lv.setOnItemClickListener { _, _, pos, _ ->
            val f = currentDir.listFiles()?.get(pos) ?: return@setOnItemClickListener
            if (f.isDirectory) { desktop.removeView(win); openGameExplorer(f) }
            else if (f.name.endsWith(".exe")) { 
                Toast.makeText(this, "Запуск игры...", Toast.LENGTH_SHORT).show()
                Thread { exeEngine.runExeFile(f.absolutePath, "${filesDir.absolutePath}/w_s") }.start() 
            }
        }
        lv.setOnItemLongClickListener { _, _, pos, _ ->
            val fileToDelete = currentDir.listFiles()?.get(pos)
            if (fileToDelete != null) {
                fileToDelete.renameTo(File(trashPath, fileToDelete.name))
                Toast.makeText(this, "Перемещено в Корзину", Toast.LENGTH_SHORT).show()
                desktop.removeView(win); openGameExplorer(currentDir)
            }
            true
        }
        win.addView(lv, LinearLayout.LayoutParams(-1, 0).apply { weight = 1f })
        desktop.addView(win, RelativeLayout.LayoutParams(950, 1300).apply { addRule(RelativeLayout.CENTER_IN_PARENT) })
        animateWindowIn(win)
    }

    override fun onBackPressed() { if (startMenu.visibility == View.VISIBLE) animateMenu(false) }
    override fun onDestroy() { super.onDestroy(); mediaPlayer?.release() }

    class DesktopAdapter(private val ctx: Context, private val list: List<LauncherApp>) : BaseAdapter() {
        override fun getCount() = list.size
        override fun getItem(p: Int) = list[p]
        override fun getItemId(p: Int) = p.toLong()
        override fun getView(p: Int, v: View?, parent: ViewGroup?): View {
            val item = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(10, 10, 10, 10); layoutParams = AbsListView.LayoutParams(-1, -2) }
            val img = ImageView(ctx).apply { setImageDrawable(list[p].icon); layoutParams = LinearLayout.LayoutParams(130, 130) }
            val txt = TextView(ctx).apply { text = list[p].title; setTextColor(Color.WHITE); textSize = 11f; gravity = Gravity.CENTER; setShadowLayer(3f, 2f, 2f, Color.BLACK) }
            item.addView(img); item.addView(txt)
            
            item.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { view.animate().scaleX(1.15f).scaleY(1.15f).setDuration(100).start() }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { view.animate().scaleX(1f).scaleY(1f).setDuration(150).start() }
                }
                false
            }
            return item
        }
    }
}

class ExeEngine {
    external fun runExeFile(p: String, pref: String): Int
    companion object { init { System.loadLibrary("exe_launcher") } }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action == Intent.ACTION_BOOT_COMPLETED && c.getSharedPreferences("app_settings", Context.MODE_PRIVATE).getBoolean("a_k", false)) {
            c.startActivity(Intent(c, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        }
    }
}
