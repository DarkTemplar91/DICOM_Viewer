package hu.bme.szasz.temalab.dicom_viewer

import android.view.MotionEvent
import kotlin.math.atan2

class RotationGestureDetector(listener: OnRotationGestureListener) {

    private val INVALID_POINTER_ID = -1

    private var fX : Float  =0.0f
    private var fY : Float  =0.0f
    private var sX : Float  =0.0f
    private var sY : Float  =0.0f

    private var ptrID1 = INVALID_POINTER_ID
    private var ptrID2 = INVALID_POINTER_ID

    var angle = 0f

    private var mListener: OnRotationGestureListener? = null

    init{
        this.mListener = listener
    }

    fun onTouchEvent(p1: MotionEvent?): Boolean {
        if(p1 == null)
            return false

        when (p1.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                ptrID1 = p1.getPointerId(p1.actionIndex)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                ptrID2 = p1.getPointerId(p1.actionIndex)
                sX = p1.getX(p1.findPointerIndex(ptrID1))
                sX = p1.getY(p1.findPointerIndex(ptrID1))
                fX = p1.getX(p1.findPointerIndex(ptrID2))
                fY = p1.getY(p1.findPointerIndex(ptrID2))
            }
            MotionEvent.ACTION_MOVE -> {
                if (ptrID1 != INVALID_POINTER_ID && ptrID2 != INVALID_POINTER_ID) {
                    val nsX = p1.getX(p1.findPointerIndex(ptrID1))
                    val nsY = p1.getY(p1.findPointerIndex(ptrID1))
                    val nfX = p1.getX(p1.findPointerIndex(ptrID2))
                    val nfY = p1.getY(p1.findPointerIndex(ptrID2))

                    angle = calculateAngle(fX, fY, sX, sY, nfX, nfY, nsX, nsY)

                    mListener?.onRotation(this)

                }
            }
            MotionEvent.ACTION_UP -> {
                ptrID1 = INVALID_POINTER_ID
            }
            MotionEvent.ACTION_POINTER_UP -> {
                ptrID2 = INVALID_POINTER_ID
            }
            MotionEvent.ACTION_CANCEL -> {
                ptrID1 = INVALID_POINTER_ID
                ptrID2 = INVALID_POINTER_ID
            }
        }

        return true
    }

    private fun calculateAngle(
        fX: Float,
        fY: Float,
        sX: Float,
        sY: Float,
        nfX: Float,
        nfY: Float,
        nsX: Float,
        nsY: Float
    ): Float {
        val angle1 = atan2((fY - sY).toDouble(), (fX - sX).toDouble()).toFloat()
        val angle2 = atan2((nfY - nsY).toDouble(), (nfX - nsX).toDouble()).toFloat()

        var angle = Math.toDegrees((angle1 - angle2).toDouble()).toFloat() % 360
        if (angle < -180f) angle += 360.0f
        if (angle > 180f) angle -= 360.0f
        return angle
    }

    interface OnRotationGestureListener {
        fun onRotation(rotationDetector: RotationGestureDetector?)
    }

}
