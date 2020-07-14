package com.mv.livebodyexample

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.mv.livebodyexample.databinding.DialogSetThresholdBinding

class SetThresholdDialogFragment : DialogFragment() {

    private var threshold: Threshold = Threshold()
    private lateinit var listener: ThresholdDialogListener

    interface ThresholdDialogListener {
        fun onDialogPositiveClick(t: Float)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val binding = DataBindingUtil.inflate<DialogSetThresholdBinding>(
                LayoutInflater.from(it),
                R.layout.dialog_set_threshold,
                null,
                false
            )

            binding.threshold = threshold

            binding.thresholdDialogConfirm.setOnClickListener {
                if (threshold.t.isNotEmpty()) {
                    listener.onDialogPositiveClick(threshold.t.toFloat())
                }
                dismiss()
            }

            AlertDialog.Builder(it).setView(binding.root).create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = context as ThresholdDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException(("$context must implement ThresholdDialogListener"))
        }
    }

    class Threshold: BaseObservable(){
        @get:Bindable
        var t: String = ""
            set(value) {
                field = value
                notifyPropertyChanged(BR.t)
            }
    }
}