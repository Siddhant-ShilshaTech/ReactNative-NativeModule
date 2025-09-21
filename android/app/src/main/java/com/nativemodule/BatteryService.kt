package com.nativemodule

import android.app.*
import android.content.*
import android.media.MediaPlayer
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.nativemodule.R
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.bluetooth.BluetoothAdapter

class BatteryService : Service() {

    private lateinit var batteryReceiver: BroadcastReceiver
    private var mediaPlayer: MediaPlayer? = null
    private var notified = false

    override fun onCreate() {
        super.onCreate()
        Log.d("BatteryService", "Service created")

        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
                val isCharging = plugged != 0

                Log.d("BatteryService", "Battery level: $level, Charging: $isCharging")

                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

                if (level in 1..15 && !isCharging && !notified) {
                    notified = true

                    if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                        val success = bluetoothAdapter.disable()
                        Log.d("BatteryService", "Bluetooth disabled: $success")
                    }

                    playSound()
                   //showStickyNotification("Battery Alert", "Battery is at $level% and Bluetooth is turned off")
                } else if (level >= 16 || isCharging) {
                    notified = false
                    if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
                        val success = bluetoothAdapter.enable()
                        Log.d("BatteryService", "Bluetooth enabled: $success")
                    }
                }
            }
        }

        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        return START_STICKY
    }

    private fun playSound() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(audioAttributes)
                    .setOnAudioFocusChangeListener { }
                    .build()

                audioManager.requestAudioFocus(focusRequest)
            } else {
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.d("BatteryService", "Audio focus granted")

                mediaPlayer = MediaPlayer.create(this, R.raw.alert)
                mediaPlayer?.setOnCompletionListener {
                    Log.d("BatteryService", "Audio completed")
                    mediaPlayer?.release()
                    mediaPlayer = null

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        audioManager.abandonAudioFocus(null)
                    }
                }

                mediaPlayer?.start()
            } else {
                Log.e("BatteryService", "Audio focus not granted")
            }
        } catch (e: Exception) {
            Log.e("BatteryService", "Error playing sound: ${e.message}", e)
        }
    }

    private fun showStickyNotification(title: String, message: String) {
        val channelId = "battery_channel"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Battery Alert Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = Notification.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun startForegroundService() {
        showStickyNotification("Battery Monitor", "Monitoring battery level")
    }

    override fun onDestroy() {
        unregisterReceiver(batteryReceiver)
        mediaPlayer?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
