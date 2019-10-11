package juniojsv.minimum

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import juniojsv.minimum.Settings.Companion.KEY_DARK_MODE
import juniojsv.minimum.extension.arrayList.removeByPackage
import juniojsv.minimum.extension.arrayList.sort
import kotlinx.android.synthetic.main.minimum_activity.*
import kotlinx.android.synthetic.main.search_header.view.*
import java.lang.ref.WeakReference

class MinimumActivity : AppCompatActivity() {
    private var apps: ArrayList<App> = ArrayList()
    private val filteredApps: ArrayList<App> = ArrayList()
    private var adapter: Adapter = Adapter(this, apps)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Settings(this).getBoolean(KEY_DARK_MODE)) {
            setTheme(R.style.AppThemeDark)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.minimum_activity)

        apps_list_view.apply {
            addHeaderView(layoutInflater.inflate(
                    R.layout.search_header, apps_list_view, false))

            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                if (search_header.text.isNotEmpty() && position > 0) startActivity(
                        filteredApps[position - 1].intent)
                else if (position > 0) startActivity(apps[position - 1].intent)
            }

            onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, position, _ ->
                if (search_header.text.isNotEmpty() && position > 0) {
                    val packageUri = Uri.parse("package:" + filteredApps[position - 1].packageName)
                    val uninstall = Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri)
                    startActivity(uninstall)
                } else if (position > 0) {
                    val packageUri = Uri.parse("package:" + apps[position - 1].packageName)
                    val uninstall = Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri)
                    startActivity(uninstall)
                }
                true
            }

            search_header.apply {
                addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(p0: Editable?) {
                    }

                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        filteredApps.clear()
                        if (p0!!.isNotEmpty()) {
                            apps.forEach {
                                if (it.packageLabel.contains(p0, true)) filteredApps.add(it)
                            }
                            this@MinimumActivity.adapter.changeList(filteredApps)
                        } else this@MinimumActivity.adapter.changeList(apps)
                        notifyAdapter()
                    }

                })
            }
        }

        GetApps(WeakReference(this)) { apps ->
            this.apps.apply {
                if (isNotEmpty()) clear()
                addAll(apps)
                notifyAdapter()
            }
            loading.visibility = View.GONE
        }.apply {
            if(apps.isEmpty()) execute()
        }

        registerReceiver(
                object : BroadcastReceiver() {
                    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                    override fun onReceive(context: Context, intent: Intent) {
                        when(intent.action) {
                            Intent.ACTION_PACKAGE_ADDED -> {
                                val appAdded = context.packageManager.getApplicationInfo(intent.dataString!!.substring(8), 0)
                                val appIntentAdded = context.packageManager.getLaunchIntentForPackage(appAdded.packageName)

                                if(appIntentAdded != null && appAdded.packageName != BuildConfig.APPLICATION_ID) {
                                    apps.apply {
                                        add(App(
                                                appAdded.loadLabel(context.packageManager).toString(),
                                                appAdded.loadIcon(context.packageManager),
                                                appIntentAdded.apply {
                                                    action = Intent.ACTION_MAIN
                                                    addCategory(Intent.CATEGORY_LAUNCHER)
                                                },
                                                appAdded.packageName
                                        ))
                                        sort()
                                    }
                                }
                                notifyAdapter(true)
                            }

                            Intent.ACTION_PACKAGE_REMOVED -> {
                                apps.removeByPackage(
                                        intent.dataString!!.substring(8)
                                )
                                notifyAdapter(true)
                            }
                        }
                    }
                },
                IntentFilter().apply {
                    addAction(Intent.ACTION_PACKAGE_ADDED)
                    addAction(Intent.ACTION_PACKAGE_REMOVED)
                    addDataScheme("package")
                }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.minimum_shortcuts, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.dial_shortcut -> {
                startActivity(Intent(Intent.ACTION_DIAL))
            }
            R.id.camera_shortcut -> {
                TakePhoto(this)
            }
            R.id.setting_shortcut -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        // Nope
    }

    fun notifyAdapter(clearSearch: Boolean = false) {
        if (apps_list_view.adapter == null) {
            apps_list_view.adapter = adapter
        } else adapter.notifyDataSetChanged()
        if (clearSearch) apps_list_view.search_header.text.clear()
    }
}
