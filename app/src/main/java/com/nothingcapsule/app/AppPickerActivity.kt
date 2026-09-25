package com.nothingcapsule.app

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Deliberately a plain ListView + BaseAdapter rather than RecyclerView — this
 * screen is opened rarely (once during setup, occasionally after installing a
 * new app) and a full installed-app list is rarely long enough to justify the
 * extra dependency and view-holder boilerplate.
 */
class AppPickerActivity : AppCompatActivity() {

    private lateinit var prefs: CapsulePrefs
    private lateinit var adapter: AppListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_picker)
        prefs = CapsulePrefs(this)

        val apps = loadUserFacingApps()
        adapter = AppListAdapter(apps, prefs.whitelistedApps.toMutableSet()) { selected ->
            prefs.whitelistedApps = selected
        }
        findViewById<ListView>(R.id.app_list).adapter = adapter
    }

    /** Only apps with a launcher entry — skips system services nobody would recognize. */
    private fun loadUserFacingApps(): List<ApplicationInfo> {
        val pm = packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .filter { it.packageName != packageName }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }
    }

    private inner class AppListAdapter(
        private val apps: List<ApplicationInfo>,
        private val selected: MutableSet<String>,
        private val onChanged: (Set<String>) -> Unit
    ) : BaseAdapter() {

        override fun getCount() = apps.size
        override fun getItem(position: Int) = apps[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_multiple_choice, parent, false)

            val app = apps[position]
            val pm = packageManager
            val label = pm.getApplicationLabel(app).toString()

            val checkedText = view.findViewById<android.widget.CheckedTextView>(android.R.id.text1)
            checkedText.text = label
            checkedText.setTextColor(resources.getColor(R.color.brand_white, theme))
            checkedText.isChecked = selected.contains(app.packageName)

            view.setOnClickListener {
                if (selected.contains(app.packageName)) {
                    selected.remove(app.packageName)
                } else {
                    selected.add(app.packageName)
                }
                checkedText.isChecked = selected.contains(app.packageName)
                onChanged(selected.toSet())
            }

            return view
        }
    }
}
