package payment.sdk.android.cardpayment.widget

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

import payment.sdk.android.sdk.R
import android.animation.AnimatorSet

class HorizontalViewFlipper @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttrs: Int = 0
) : FrameLayout(context, attrs, defStyleAttrs) {

    private val rotateLeftOut: Animator by lazy {
        AnimatorInflater.loadAnimator(context, ROTATE_LEFT_OUT).apply {
            setTarget(frontFace)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animator: Animator) {
                    backFace.visibility = View.VISIBLE
                    frontFace.visibility = View.GONE
                }
            })
        }
    }

    private val rotateRightIn: Animator by lazy {
        AnimatorInflater.loadAnimator(context, ROTATE_RIGHT_IN).apply {
            setTarget(backFace)
        }
    }

    private val rotateRightOut: Animator by lazy {
        AnimatorInflater.loadAnimator(context, ROTATE_RIGHT_OUT).apply {
            setTarget(backFace)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animator: Animator) {
                    backFace.visibility = View.GONE
                    frontFace.visibility = View.VISIBLE
                }
            })
        }
    }
    private val flipLeftOut: Animator by lazy {
        AnimatorInflater.loadAnimator(context, ROTATE_LEFT_IN).apply {
            setTarget(frontFace)
        }
    }

    private val animationCameraDistance: Float by lazy {
        resources.displayMetrics.density * 1280f * 10f
    }

    private lateinit var frontFace: View
    private lateinit var backFace: View
    private var backFaceViewStub: View? = null

    /**
     * We don't want to spend time on inflating back face of card so it is defined as view stub
     * It is inflated whilst we first time show it (via flip animation)
     * See @link {HorizontalViewFlipper.inflateCardBackViewStubIfNeeded}
     */
    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount != 2) {
            throw IllegalArgumentException("Children size should be 2")
        }
        /**
         * https://developer.android.com/reference/android/view/View.html#setCameraDistance(float)
         */
        frontFace = getChildAt(0).apply {
            cameraDistance = animationCameraDistance
        }
        backFaceViewStub = getChildAt(1)
    }

    /**
     * Flip card back face
     */
    fun flipRightToLeft(onAnimationEndCallback: (() -> Unit)? = null) {
        inflateCardBackViewStubIfNeeded()
        if (rotateLeftOut.isRunning || rotateRightIn.isRunning) {
            return
        }
        backFace.visibility = View.VISIBLE
        frontFace.visibility = View.VISIBLE

        val flipAnimation = AnimatorSet()
        flipAnimation.playTogether(rotateLeftOut, rotateRightIn)
        onAnimationEndCallback?.let { callback ->
            flipAnimation.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    callback()
                }
            })
        }
        flipAnimation.start()

    }

    private fun inflateCardBackViewStubIfNeeded() {
        backFaceViewStub?.let {
            it.visibility = View.VISIBLE
            backFaceViewStub = null
            backFace = findViewById<View>(R.id.card_back_face).apply {
                cameraDistance = animationCameraDistance
            }
        }
    }

    /**
     * Flip card front face
     */

    fun flipLeftToRight(onAnimationEndCallback: (() -> Unit)? = null) {
        if (rotateRightOut.isRunning || flipLeftOut.isRunning) {
            return
        }
        backFace.visibility = View.VISIBLE
        frontFace.visibility = View.VISIBLE

        val flipAnimation = AnimatorSet()
        flipAnimation.playTogether(rotateRightOut, flipLeftOut)
        onAnimationEndCallback?.let { callback ->
            flipAnimation.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    callback()
                }
            })
        }
        flipAnimation.start()
    }

    companion object {
        private val ROTATE_LEFT_OUT: Int = R.animator.rotate_left_out
        private val ROTATE_RIGHT_IN: Int = R.animator.rotate_right_in
        private val ROTATE_RIGHT_OUT: Int = R.animator.rotate_right_out
        private val ROTATE_LEFT_IN: Int = R.animator.rotate_left_in
    }

}