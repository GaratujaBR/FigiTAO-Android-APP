package com.example.figitaoapp

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.UnsupportedEncodingException
import java.util.*

class MainActivity : AppCompatActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var textViewInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textViewInfo = findViewById(R.id.textViewInfo)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not supported on this device", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
    }

    override fun onResume() {
        super.onResume()
        val intentFiltersArray = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED).apply { addCategory(Intent.CATEGORY_DEFAULT) },
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED).apply { addCategory(Intent.CATEGORY_DEFAULT) },
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply { addCategory(Intent.CATEGORY_DEFAULT) }
        )

        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, null)
        Log.d("NFC", "onResume: NFC foreground dispatch enabled")
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
        Log.d("NFC", "onPause: NFC foreground dispatch disabled")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val action = intent.action
        Log.d("NFC", "onNewIntent: NFC intent received with action $action")
        Toast.makeText(this, "NFC intent received with action $action", Toast.LENGTH_SHORT).show()

        if (action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            action == NfcAdapter.ACTION_NDEF_DISCOVERED) {
            handleIntent(intent)
        } else {
            Log.d("NFC", "onNewIntent: Action is null, attempting to handle intent anyway")
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent) {
        Log.d("NFC", "handleIntent: Handling NFC intent")
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            readNdefMessage(tag)
        } else {
            Log.d("NFC", "handleIntent: No tag found in intent")
            Toast.makeText(this, "No tag found in intent", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readNdefMessage(tag: Tag) {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            val ndefMessage = ndef.cachedNdefMessage
            ndef.close()

            if (ndefMessage != null) {
                val records = ndefMessage.records
                for (record in records) {
                    if (record.tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.type, NdefRecord.RTD_TEXT)) {
                        try {
                            val payload = record.payload
                            val textEncoding = if ((payload[0].toInt() and 128) == 0) "UTF-8" else "UTF-16"
                            val languageCodeLength = payload[0].toInt() and 63
                            val text = String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, charset(textEncoding))
                            Log.d("NFC", "NDEF Text: $text")
                            textViewInfo.text = text
                        } catch (e: UnsupportedEncodingException) {
                            Log.e("NFC", "Unsupported Encoding", e)
                        }
                    }
                }
            } else {
                Log.d("NFC", "No NDEF message found!")
                Toast.makeText(this, "No NDEF message found!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d("NFC", "NDEF is not supported by this Tag.")
            Toast.makeText(this, "NDEF is not supported by this Tag.", Toast.LENGTH_SHORT).show()
        }
    }
}
