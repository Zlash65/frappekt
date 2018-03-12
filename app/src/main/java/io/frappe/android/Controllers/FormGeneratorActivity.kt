package io.frappe.android.Controllers

import android.accounts.AccountManager
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.*
import org.json.JSONObject
import java.util.*
import android.view.MenuItem
import android.view.Menu
import io.frappe.android.R
import io.frappe.android.UI.FormViewAdapter
import io.frappe.android.Utils.DocField
import io.frappe.android.Utils.FormUtils

open class FormGeneratorActivity : BaseCompatActivity() {

    internal lateinit var mRecyclerView: RecyclerView
    var recyclerAdapter: FormViewAdapter? = null
    var recyclerModels = ArrayList<DocField>()
    var docname: String = ""
    var progressBar: ProgressBar? = null
    var excludeName = ArrayList<String>().apply {
        add("produce_name")
    }

    lateinit var save: MenuItem
    lateinit var edit: MenuItem

    companion object {
        var docData = JSONObject()
        lateinit var doctype: String
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runOnCreate()
        setContentView(R.layout.activity_form)

        // make progress bar visible while loading data
        progressBar = this.findViewById(R.id.delay_progress_bar)
        progressBar?.visibility = View.VISIBLE

        // set meta
        if (intent.hasExtra("DocType")) {
            doctype = this.doctype
            setupDocType(intent.getStringExtra("DocType"))
        }

        // if docname, fetch doc data
        if (intent.hasExtra("DocName")) {
            this.docname = intent.getStringExtra("DocName")
            FormUtils(this).fetchDoc(this.doctype!!, this.docname, "[\"*\"]", setupCallback)
        }

        validateDocMeta()

        setupRecycler()
    }

    open fun runOnCreate() {}

    // Add DocField object of Meta data
    fun validateDocMeta() {
        val fields = docMeta?.getJSONArray("fields")!!
        var pushDocMeta: DocField
        for (i in 0 until fields.length() - 1) {
            pushDocMeta = DocField(fields.getJSONObject(i))
            if (pushDocMeta.fieldname != null && !excludeName.contains(pushDocMeta!!.fieldname))
                recyclerModels.add(pushDocMeta)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu);

        menuInflater.inflate(R.menu.form_view_menu, menu)
        edit = menu.findItem(R.id.edit_form)
        save = menu.findItem(R.id.save_form)
        if (docname.isBlank()) edit.isVisible = true

        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var bool = item.toString() == "Edit"
        edit.isVisible = !bool
        save.isVisible = bool

        when (item?.itemId) {
            R.id.edit_form -> {
                return true
            }
            R.id.save_form -> {
                return true
            }
            else -> return false
        }
    }

    fun setupRecycler() {
        mRecyclerView = findViewById(R.id.form_recycler_view)
        mRecyclerView.visibility = View.INVISIBLE

        val mLayoutManager = LinearLayoutManager(this)
        mRecyclerView.setLayoutManager(mLayoutManager)

        recyclerAdapter = FormViewAdapter(recyclerModels, docMeta!!)
        mRecyclerView.adapter = recyclerAdapter

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true)

        val vto = mRecyclerView.getViewTreeObserver() // wait for all views to be loaded
        vto.addOnGlobalLayoutListener(ViewTreeObserver.OnGlobalLayoutListener {
            if (mRecyclerView.adapter.itemCount == recyclerModels.size
                    && docname.isBlank()) {
                progressBar?.visibility = View.GONE
                viewIterator()
            }
        })
    }

    override fun onBackPressed() {
        finish()
    }

    fun viewIterator() {
        var holderArray = FormViewAdapter.holderArray
        for (i in 0..holderArray.size - 1) {
            updateFieldData(holderArray[i])
        }

        // make progress bar visible while loading data
        progressBar?.visibility = View.GONE
        mRecyclerView.visibility = View.VISIBLE
    }

    // Set value for the created fields
    fun updateFieldData(holder: FormViewAdapter.ViewHolder) {

        var jsonObject = recyclerModels[holder.position]
        holder.label.text = jsonObject.label + " : "
        val viewType = holder.value.javaClass.simpleName

        if (viewType == "EditText" && (jsonObject.fieldtype == "DateTime" || jsonObject.fieldtype == "Date")) {
            val value = holder.value as EditText
            if (docname.isNotBlank()) value.setText(docData.getString(jsonObject.fieldname))
            FormUtils(this).displayCalender(value)
            value.inputType = 0
        } else if (viewType == "EditText") {
            val value = holder.value as EditText
            if (docname.isNotBlank()) value.setText(docData.getString(jsonObject.fieldname))
            value.inputType = 0
        } else if (viewType == "TextView") {
            val value = holder.value as TextView
            if (docname.isNotBlank()) value.text = docData.getString(jsonObject.fieldname)
        } else if (viewType == "CheckBox") {
            var value = holder.value as CheckBox
            if (docname.isNotBlank()) value.setChecked(docData.getInt(jsonObject.fieldname) == 1)
        }

    }

    // callback for fetching docData
    val setupCallback: (JSONObject) -> Unit = { data ->
        run {
            docData = data
            this.findViewById<TextView>(R.id.docname).setText(docname)
            val mAccountManager = AccountManager.get(this.applicationContext)
            val accounts = mAccountManager?.getAccountsByType(applicationContext.getString(R.string.package_name))
            val user = accounts?.get(0)?.name
            if (FormUtils(this).isOwner(user!!)) edit.isVisible = true
            this.viewIterator()
        }
    }
}