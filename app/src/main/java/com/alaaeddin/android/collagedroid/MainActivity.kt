package com.alaaeddin.android.collagedroid

import android.os.Bundle
import androidx.annotation.DrawableRes
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.alaaeddin.android.collagedroid.R.drawable.android_kotlin
import com.alaaeddin.android.collagedroid.R.drawable.android_happy
import com.alaaeddin.android.collagedroid.R.drawable.android_jetpack

class MainActivity : AppCompatActivity() {

  private lateinit var droidImage: ImageView
  private lateinit var navigation: BottomNavigationView

  private val mOnNavigationItemSelectedListener = OnNavigationItemSelectedListener { item ->
    when (item.itemId) {
      R.id.navigation_first -> {
        changeScreen(TemplateType.FIRST, android_jetpack)

        return@OnNavigationItemSelectedListener true
      }
      R.id.navigation_second -> {
        changeScreen(TemplateType.SECOND, android_kotlin)

        return@OnNavigationItemSelectedListener true
      }
      R.id.navigation_third -> {
        changeScreen(TemplateType.THIRD, android_happy)
        return@OnNavigationItemSelectedListener true
      }
    }
    false
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    bindUI()
    navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    navigation.selectedItemId = R.id.navigation_first
  }

  private fun changeScreen(templateType: TemplateType, @DrawableRes drawable: Int) {
    val fragment = CollageFragment.newInstance(templateType)
    val tag = fragment.javaClass.simpleName

    supportFragmentManager
        .beginTransaction()
        .replace(R.id.fragment_container, fragment, tag)
        .commit()

    changeDroidImage(drawable)
  }

  private fun changeDroidImage(@DrawableRes drawable: Int) {
    droidImage.setImageResource(drawable)
    animateDroidImage()
  }

  private fun animateDroidImage() {
    val anim = RotateAnimation(0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
    with(anim) {
      interpolator = LinearInterpolator()
      repeatCount = 1
      duration = 700
    }

    droidImage.startAnimation(anim)
  }

  private fun bindUI() {
    navigation = findViewById(R.id.navigation)
    droidImage = findViewById(R.id.droid_image)
  }
}
