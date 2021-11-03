package com.mod5group1.autohelm

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import kotlin.math.PI

/**
 * A simple [Fragment] subclass.
 * Use the [AutohelmFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AutohelmFragment : Fragment() {

    private val viewModel: MainActivity.AutohelmViewModel by activityViewModels()
    private var movingCompass = false

    //Not proud of this mess but eh
    private var startRotation = 0f
    private var tapStartRotation = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_autohelm, container, false)

        val compass = view.findViewById<ImageView>(R.id.compassForeground)
        val trajectoryField = view.findViewById<EditText>(R.id.trajectoryField)
        val currentTrajectoryText = view.findViewById<TextView>(R.id.currentTrajectory)
        val plannedTrajectoryText = view.findViewById<TextView>(R.id.plannedTrajectory)
        val commitButton = view.findViewById<Button>(R.id.commitTrajectory)
        val resetButton = view.findViewById<Button>(R.id.resetTrajectory)

        trajectoryField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                movingCompass = true
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (trajectoryField.hasFocus()) {
                    val trajectory = s.toString().toFloatOrNull()
                    if (trajectory != null && trajectory > -180 && trajectory < 180) {
                        compass.rotation = trajectory
                        trajectoryField.error = null
                    } else {
                        trajectoryField.error = "Input a value between -180 and 180 degrees"
                    }
                }

            }

            override fun afterTextChanged(s: Editable?) {
                movingCompass = false
            }
        })

        commitButton.setOnClickListener {
            val trajectory = trajectoryField.text.toString().toFloatOrNull()
            if (trajectory != null && trajectory > -180 && trajectory < 180) {
                plannedTrajectoryText.text = "Planned trajectory: $trajectory"
                viewModel.setPlannedTrajectory(trajectory)
            } else {
                Snackbar.make(
                    commitButton,
                    "Incorrect trajectory value entered",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        resetButton.setOnClickListener {
            compass.rotation = 0f
            plannedTrajectoryText.text = "No planned trajectory"
            trajectoryField.setText("0")
        }

        compass.isClickable = true

        compass.rotation =
            viewModel.currentTrajectory.value ?: viewModel.plannedTrajectory.value ?: 0f

        if (viewModel.currentTrajectory.value != null) {
            currentTrajectoryText.text = "Current trajectory: ${viewModel.currentTrajectory.value}"
        }
        if (viewModel.plannedTrajectory.value != null) {
            plannedTrajectoryText.text = "Planned trajectory: ${viewModel.plannedTrajectory.value}"
            trajectoryField.setText(viewModel.plannedTrajectory.value.toString())
        }

        compass.setOnTouchListener { _, event ->
            when (event.action) {
                ACTION_DOWN -> {
                    movingCompass = true
                    startRotation = compass.rotation
                    tapStartRotation = kotlin.math.atan2(
                        compass.pivotY - event.rawY,
                        compass.pivotX - event.rawX
                    ) * 180f / PI.toFloat()
                }
                ACTION_UP -> movingCompass = false
                ACTION_MOVE -> {
                    var newTrajectory = kotlin.math.atan2(
                        compass.pivotY - event.rawY,
                        compass.pivotX - event.rawX
                    ) * 180f / PI.toFloat()
                    newTrajectory += startRotation - tapStartRotation

                    while (newTrajectory < -180) newTrajectory += 360
                    while (newTrajectory > 180) newTrajectory -= 360

                    trajectoryField.setText(
                        String.format("%.2f", newTrajectory)
                    )

                    compass.rotation = newTrajectory
                }
            }
            return@setOnTouchListener true
        }


        viewModel.currentTrajectory.observe(requireActivity()) {
            if (!movingCompass) {
                compass.rotation = it
            }
            currentTrajectoryText.text = "Current trajectory: $it"
        }

        return view
    }

}
