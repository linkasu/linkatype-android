package ru.ibakaidov.distypepro.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.LayoutRes

abstract class Component @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    init {
        LayoutInflater.from(context).inflate(layoutId(), this, true)
    }

    @LayoutRes
    abstract fun layoutId(): Int

    abstract fun initUi()

    public override fun onFinishInflate() {
        super.onFinishInflate()
        initUi()
    }
}
