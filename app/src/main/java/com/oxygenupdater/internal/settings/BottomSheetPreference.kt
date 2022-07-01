package com.oxygenupdater.internal.settings

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.oxygenupdater.R
import com.oxygenupdater.adapters.BottomSheetItemAdapter
import com.oxygenupdater.extensions.getString
import com.oxygenupdater.utils.Logger.logInfo
import com.oxygenupdater.utils.Logger.logVerbose
import kotlin.math.min

/**
 * Overridden class to add custom functionality (setting title/message, custom on click listeners for dismissing).
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Suppress("unused")
class BottomSheetPreference : Preference {

    private lateinit var mContext: Context
    private lateinit var dialogLayout: View
    private lateinit var adapter: BottomSheetItemAdapter
    private lateinit var dialog: BottomSheetDialog

    private var mOnChangeListener: OnPreferenceChangeListener? = null
    private var secondaryKey: String? = null
    private var title: String? = null
    private var caption: String? = null
    private var itemList: MutableList<BottomSheetItem> = ArrayList()
    private var valueSet = false

    var value: Any? = null
        private set
    var secondaryValue: Any? = null
        private set

    private constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs, defStyleAttr, defStyleRes)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr, 0)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        init(context, attrs, 0, 0)
    }

    constructor(context: Context) : super(context) {
        init(context, null, 0, 0)
    }

    /**
     * Initialise the preference
     *
     * @param context      the context
     * @param attrs        the attributes
     * @param defStyleAttr default style attributes
     * @param defStyleRes  default style resource
     */
    private fun init(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) {
        this.mContext = context

        readAttrs(attrs, defStyleAttr, defStyleRes)

        if (secondaryKey == null) {
            secondaryKey = key + "_id"
        }

        setupDialog()
    }

    /**
     * Setup the internal [BottomSheetDialog]
     */
    @SuppressLint("InflateParams")
    private fun setupDialog() = BottomSheetDialog(mContext).apply {
        val contentView = LayoutInflater.from(mContext).inflate(
            R.layout.bottom_sheet_preference,
            null, false
        ).apply {
            dialogLayout = this

            setText(findViewById(R.id.dialog_title), title)
            setText(findViewById(R.id.dialog_caption), caption)

            adapter = BottomSheetItemAdapter(mContext, key, secondaryKey) {
                setValueItem(it)
            }.apply {
                submitList(itemList)
                findViewById<RecyclerView>(R.id.bottomSheetRecyclerView).let { recyclerView ->
                    // Performance optimization
                    recyclerView.setHasFixedSize(true)
                    recyclerView.adapter = this
                }
            }

            logVerbose(TAG, "Setup dialog with title='$title', subtitle='$caption', and '${itemList.size}' items")
        }

        setContentView(contentView)
        // Open up the dialog fully by default
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog = this
    }

    /**
     * Reads attributes defined in XML and sets relevant fields
     *
     * @param attrs        the attributes
     * @param defStyleAttr default style attribute
     * @param defStyleRes  default style resource
     */
    private fun readAttrs(attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val a = mContext.obtainStyledAttributes(attrs, R.styleable.BottomSheetPreference, defStyleAttr, defStyleRes)

        title = a.getString(R.styleable.BottomSheetPreference_title, R.styleable.BottomSheetPreference_android_title)
        caption = a.getString(R.styleable.BottomSheetPreference_caption)
        secondaryKey = a.getString(R.styleable.BottomSheetPreference_secondaryKey)

        val entries = a.getTextArray(R.styleable.BottomSheetPreference_android_entries)
        val entryValues = a.getTextArray(R.styleable.BottomSheetPreference_android_entryValues)
        val titleEntries = a.getTextArray(R.styleable.BottomSheetPreference_titleEntries)
        val subtitleEntries = a.getTextArray(R.styleable.BottomSheetPreference_subtitleEntries)
        val resourceId = a.getResourceId(R.styleable.BottomSheetPreference_secondaryEntryValues, 0)

        itemList = ArrayList()

        val intEntryValues = if (resourceId != 0) mContext.resources.getIntArray(resourceId) else null
        val numberOfItems = entries?.size ?: (titleEntries?.size ?: 0)

        for (i in 0 until numberOfItems) {
            val item = BottomSheetItem(value = entryValues[i].toString())

            val title = if (entries != null) entries[i] else titleEntries!![i]
            val subtitle = subtitleEntries?.get(i)
            val secondaryValue = intEntryValues?.get(i)

            if (title != null) {
                item.title = title.toString()
            }

            if (subtitle != null) {
                item.subtitle = subtitle.toString()
            }

            if (secondaryValue != null) {
                item.secondaryValue = secondaryValue
            }

            itemList.add(item)
        }

        a.recycle()
    }

    /**
     * Utility method that sets text if the supplied text is not null, otherwise hides the [TextView]
     *
     * @param textView the TextView
     * @param text     the text
     */
    private fun setText(textView: TextView, text: String?) = if (text != null) {
        textView.isVisible = true
        textView.text = text
    } else {
        textView.isVisible = false
    }

    /**
     * Finds index of item with newValue or newSecondaryValue and calls [setValueIndex]
     *
     * @param newValue          the new value
     * @param newSecondaryValue the new integer value
     */
    private fun setValues(newValue: Any?, newSecondaryValue: Any?) {
        var selectedIndex = -1

        itemList.forEachIndexed { index, item ->
            val value = item.value
            val secondaryValue = item.secondaryValue

            if (value != null && value == newValue || secondaryValue != null && secondaryValue == newSecondaryValue) {
                selectedIndex = index
                return@forEachIndexed
            }
        }

        if (selectedIndex != -1) {
            setValueIndex(selectedIndex)
        }
    }

    fun setValueIndex(selectedIndex: Int) = setValueItem(itemList[selectedIndex])

    /**
     * Sets the value of the key. secondaryValue is optional.
     *
     * Also marks previous item as unselected and new item as selected
     *
     * @param item the selected item
     */
    private fun setValueItem(item: BottomSheetItem) {
        val newValue = item.value
        val newSecondaryValue = item.secondaryValue

        // Always persist/notify the first time.
        val changed = newValue != value
        if (changed || !valueSet) {
            value = newValue
            secondaryValue = newSecondaryValue
            valueSet = true
            PrefManager.putPreference(key, newValue)

            if (newSecondaryValue != null) {
                PrefManager.putPreference(secondaryKey, newSecondaryValue)
            }

            if (changed) {
                // redraw previous selected and new selected layouts
                adapter.notifyDataSetChanged()
                onChange()
            }
        }
    }

    fun setSecondaryKey(secondaryKey: String?) {
        logInfo(TAG, "Updating secondaryKey: $secondaryKey")
        this.secondaryKey = secondaryKey
    }

    override fun setTitle(title: CharSequence?) {
        logInfo(TAG, "Updating dialog title: $title")
        this.title = title.toString()
        setText(dialogLayout.findViewById(R.id.dialog_title), title.toString())
        super.setTitle(title)
    }

    override fun onClick() = dialog.show()

    /**
     * Sets the callback to be invoked when this preference is changed by the user (but before
     * the internal state has been updated).
     *
     * @param onPreferenceChangeListener The callback to be invoked
     */
    override fun setOnPreferenceChangeListener(
        onPreferenceChangeListener: OnPreferenceChangeListener?
    ) = super.setOnPreferenceChangeListener(onPreferenceChangeListener).also {
        mOnChangeListener = onPreferenceChangeListener
    }

    override fun onSetInitialValue(defaultValue: Any?) = setValues(
        value?.let {
            PrefManager.getPreference(key, it, null)
        } ?: PrefManager.getPreference<Any?>(key, null),
        secondaryValue?.let {
            PrefManager.getPreference(secondaryKey, it, null)
        } ?: PrefManager.getPreference<Any?>(secondaryKey, null)
    )

    override fun onSaveInstanceState() = super.onSaveInstanceState().let { superState ->
        if (isPersistent) {
            // No need to save instance state since it's persistent
            superState
        } else {
            SavedState(superState).also {
                it.value = value
                it.secondaryValue = secondaryValue
            }
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state.javaClass != SavedState::class.java) { // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }

        val myState = state as SavedState
        super.onRestoreInstanceState(myState.superState)

        setValues(myState.value, myState.secondaryValue)
    }

    fun setCaption(caption: String?) {
        logInfo(TAG, "Updating dialog caption: $caption")
        this.caption = caption
        setText(dialogLayout.findViewById(R.id.dialog_caption), caption)
    }

    fun setEntries(entries: Array<CharSequence>) {
        logInfo(TAG, "Updating titles for ${entries.size} items")

        // don't care if items and entries aren't equal in length
        // replace title for all indices that are common
        val length = min(itemList.size, entries.size)
        for (i in 0 until length) {
            itemList[i].title = entries[i].toString()
        }

        adapter.submitList(itemList)

        onSetInitialValue(null)
    }

    fun setTitleEntries(titles: Array<CharSequence>) {
        logInfo(TAG, "Updating titles for ${titles.size} items")

        // don't care if items and titles aren't equal in length
        // replace title for all indices that are common
        val length = min(itemList.size, titles.size)
        for (i in 0 until length) {
            itemList[i].title = titles[i].toString()
        }

        adapter.submitList(itemList)

        onSetInitialValue(null)
    }

    fun setSubtitleEntries(subtitles: Array<CharSequence>) {
        logInfo(TAG, "Updating subtitles for ${subtitles.size} items")

        // don't care if items and subtitles aren't equal in length
        // replace subtitle for all indices that are common
        val length = min(itemList.size, subtitles.size)
        for (i in 0 until length) {
            itemList[i].subtitle = subtitles[i].toString()
        }

        adapter.submitList(itemList)
    }

    fun setEntryValues(entryValues: Array<CharSequence>) {
        logInfo(TAG, "Updating entryValues for ${entryValues.size} items")

        // don't care if items and entryValues aren't equal in length
        // replace value for all indices that are common
        val length = min(itemList.size, entryValues.size)
        for (i in 0 until length) {
            itemList[i].value = entryValues[i].toString()
        }

        adapter.submitList(itemList)
    }

    fun setSecondaryEntryValues(objectEntryValues: Array<Long?>) {
        logInfo(TAG, "Updating secondaryValues for ${objectEntryValues.size} items")

        // don't care if items and objectEntryValues aren't equal in length
        // replace secondaryValue for all indices that are common
        val length = min(itemList.size, objectEntryValues.size)
        for (i in 0 until length) {
            itemList[i].secondaryValue = objectEntryValues[i]
        }

        adapter.submitList(itemList)
    }

    fun setItemList(itemList: List<BottomSheetItem>) {
        logInfo(TAG, "Populating itemList with ${itemList.size} items")

        // copy list instead of shallow-referencing it
        this.itemList = ArrayList()
        this.itemList.addAll(itemList)

        adapter.submitList(this.itemList)

        onSetInitialValue(null)
    }

    /**
     * Called when value changes. Updates summary, closes the [dialog], and notifies on change listeners
     */
    private fun onChange() {
        summary = value.toString()

        dialog.cancel()
        mOnChangeListener?.onPreferenceChange(this, value)
    }

    private class SavedState : BaseSavedState {

        var value: Any? = null
        var secondaryValue: Any? = null

        constructor(source: Parcel) : super(source) {
            value = source.readString()
            secondaryValue = source.readLong()
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(dest: Parcel, flags: Int) = super.writeToParcel(dest, flags).let {
            dest.writeString(value.toString())
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel) = SavedState(parcel)

            override fun newArray(size: Int) = arrayOfNulls<SavedState?>(size)
        }

        override fun describeContents() = 0
    }

    companion object {
        private const val TAG = "BottomSheetPreference"
    }
}
