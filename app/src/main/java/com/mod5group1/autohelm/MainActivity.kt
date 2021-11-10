package com.mod5group1.autohelm

import android.bluetooth.BluetoothSocket
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import kotlin.math.absoluteValue
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private val viewModel: AutohelmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel.currentTrajectory.observe(this) {
            //TODO: handle new trajectory (or push this to the bluetoothfragment
            println("New current trajectory: $it")
        }
        viewModel.bluetoothSocket.observe(this) {
            val thread = HandleConnectionThread(it)
            thread.start()
            viewModel.setConnectionThread(thread)
        }
        viewModel.plannedTrajectory.observe(this) {
            println("New planned trajectory: $it")
            val connectionThread = viewModel.connectionThread.value
            if (connectionThread != null) {
//                connectionThread.sendMessage("trajectory $it")
                connectionThread.sendInt(it.toInt())
            } else {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "You're not connected to the autohelm yet, maybe do that first",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }


        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener {
            val selectedFragment: Fragment? = when (it.itemId) {
                R.id.nav_bluetooth -> BluetoothFragment()
                R.id.nav_helm -> AutohelmFragment()
                else -> null
            }
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, selectedFragment!!)
                .commit()
            return@setOnItemSelectedListener true
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, AutohelmFragment())
            .commit()

    }


    inner class HandleConnectionThread(private val socket: BluetoothSocket) : Thread() {


        override fun run() {
            socket.use {
                outer@ while (true) {
                    val resultArray = Array(8) { 0 }
                    val read = socket.inputStream.read()

                    println("RECEIVED INT $read")
                    var negative = false

                    var readDone = false
                    reading@ while (!readDone) {
                        val read1 = socket.inputStream.read()
                        when (read1) {
                            48, 255 -> {
                                readDone = true
                                negative = false
                            }
                            49, 254 -> {
                                readDone = true
                                negative = true
                            }
                            13, 10 -> continue@reading
                            else -> {
                                println("received some cringe numbers")
                                continue@outer
                            }
                        }
                    }


                    println("RECEIVED NEGATIVITY $negative")

                    for (i in resultArray.indices) {
                        var readDone = false
                        reading@ while (!readDone) {
                            val read1 = socket.inputStream.read()
                            when (read1) {
                                48, 255 -> {
                                    readDone = true
                                    resultArray[i] = 0
                                }
                                49, 254 -> {
                                    readDone = true
                                    resultArray[i] = 1
                                }
                                13, 10 -> continue@reading
                                else -> {
                                    println("received some cringe numbers")
                                    continue@outer
                                }
                            }
                        }
                        println("RECEIVED BIT ${resultArray[i]}")
                    }
                    // resultArray is now filled, process it.
                    var result = 0
                    for ((i, number) in resultArray.withIndex()) {
                        if (number == 1) {
                            result += 2.0.pow(i).toInt()
                        }
                    }
                    if (negative) result *= -1


                    println("Recieved message from bluetooth connection: $result")
                    //TODO: HANDLE THIS!!!!!

                }
            }
        }

        fun sendInt(message: Int) {
            println("Sending message to bluetooth connection: $message")

            socket.outputStream.write(if (message < 0) 1 else 0)

            var messageString = Integer.toBinaryString(message.absoluteValue).padStart(8, '0')
            println("Sending this in binary to arduino: ${messageString.substring(8 - messageString.length)}")

            for (char in messageString.substring(8 - messageString.length).reversed()) {

                socket.outputStream.write(if (char == '0') 0 else 100)
            }
        }
    }

    class AutohelmViewModel : ViewModel() {
        private val mutablePlannedTrajectory = MutableLiveData<Float>()
        private val mutableCurrentTrajectory = MutableLiveData<Float>()
        private val mutableBluetoothSocket = MutableLiveData<BluetoothSocket>()
        private val mutableConnectionThread = MutableLiveData<HandleConnectionThread>()

        val plannedTrajectory: LiveData<Float> get() = mutablePlannedTrajectory
        val currentTrajectory: LiveData<Float> get() = mutableCurrentTrajectory
        val bluetoothSocket: LiveData<BluetoothSocket> get() = mutableBluetoothSocket
        val connectionThread: LiveData<HandleConnectionThread> get() = mutableConnectionThread

        fun setPlannedTrajectory(trajectory: Float) {
            mutablePlannedTrajectory.value = trajectory
        }

        fun setCurrentTrajectory(trajectory: Float) {
            mutableCurrentTrajectory.value = trajectory
        }

        fun setBluetoothSocket(socket: BluetoothSocket) {
            mutableBluetoothSocket.value = socket
        }

        fun setConnectionThread(thread: HandleConnectionThread) {
            mutableConnectionThread.value = thread
        }
    }

}
