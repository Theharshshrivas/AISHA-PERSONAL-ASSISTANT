
package com.harsh.aisha

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun ReminderScreen(onSpeak: (String) -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { ReminderStore(context) }

    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedTime by remember {
        mutableStateOf(System.currentTimeMillis() + 60 * 60 * 1000L)
    }
    var reminders by remember { mutableStateOf(store.getAll()) }
    var status by remember { mutableStateOf("") }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        status = if (granted) {
            "Notifications allowed."
        } else {
            "Notifications disabled. Enable them in Android Settings."
        }
    }

    fun requestNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun refresh() {
        reminders = store.getAll()
    }

    fun chooseDateAndTime() {
        val initial = Calendar.getInstance().apply {
            timeInMillis = selectedTime
        }

        DatePickerDialog(
            context,
            { _, year, month, day ->
                val chosen = Calendar.getInstance().apply {
                    timeInMillis = selectedTime
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }

                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        chosen.set(Calendar.HOUR_OF_DAY, hour)
                        chosen.set(Calendar.MINUTE, minute)
                        chosen.set(Calendar.SECOND, 0)
                        chosen.set(Calendar.MILLISECOND, 0)
                        selectedTime = chosen.timeInMillis
                    },
                    chosen.get(Calendar.HOUR_OF_DAY),
                    chosen.get(Calendar.MINUTE),
                    false
                ).show()
            },
            initial.get(Calendar.YEAR),
            initial.get(Calendar.MONTH),
            initial.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
        }.show()
    }

    SectionTitle("REMINDERS")

    Text(
        "Schedule study tasks and get a phone notification.",
        color = Color.LightGray,
        fontSize = 13.sp
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF201A35)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reminder title") },
                placeholder = { Text("Example: Revise Accountancy") },
                singleLine = true
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Details (optional)") },
                placeholder = { Text("Chapter 3 practice questions") },
                minLines = 2,
                maxLines = 3
            )

            TextButton(onClick = { chooseDateAndTime() }) {
                Text(
                    "📅 " + DateFormat.getDateTimeInstance(
                        DateFormat.MEDIUM,
                        DateFormat.SHORT
                    ).format(Date(selectedTime))
                )
            }

            Button(
                onClick = {
                    if (store.add(title, note, selectedTime)) {
                        title = ""
                        note = ""
                        selectedTime = System.currentTimeMillis() + 60 * 60 * 1000L
                        refresh()
                        status = "Reminder saved. Android may deliver it a little late."
                        requestNotificationPermission()
                        onSpeak("Reminder set kar diya.")
                    } else {
                        status = "Title bharo aur future ka time select karo."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("＋ Save Reminder")
            }

            if (status.isNotBlank()) {
                Text(status, color = Color(0xFF55E4D0), fontSize = 12.sp)
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "UPCOMING (${reminders.size})",
            color = Color(0xFF55E4D0),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        TextButton(onClick = { requestNotificationPermission() }) {
            Text("Enable notifications")
        }
    }

    if (reminders.isEmpty()) {
        InfoCard("No reminders yet", "Create a reminder above to save it on this device.")
    } else {
        reminders.forEach { reminder ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF201A35)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        reminder.title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (reminder.note.isNotBlank()) {
                        Text(reminder.note, color = Color.LightGray, fontSize = 13.sp)
                    }

                    Text(
                        DateFormat.getDateTimeInstance(
                            DateFormat.MEDIUM,
                            DateFormat.SHORT
                        ).format(Date(reminder.triggerAtMillis)),
                        color = Color(0xFF55E4D0),
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                store.delete(reminder.id)
                                refresh()
                                status = "Reminder deleted and alarm cancelled."
                            }
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }

    InfoCard(
        "Timing note",
        "This version uses inexact Android alarms. Android 13+ requires notification permission. Battery saver and device policies may delay notifications."
    )
}
