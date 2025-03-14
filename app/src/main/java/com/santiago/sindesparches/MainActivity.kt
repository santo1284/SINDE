package com.santiago.sindesparches

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.santiago.sindesparches.ui.theme.SinDesparchesTheme

class MainActivity : ComponentActivity() {

    private lateinit var navControlller: NavHostController
    private lateinit var auth: FirebaseAuth
    private var db = Firebase.firestore
    private lateinit var analytics: FirebaseAnalytics

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.splash_screen)
        super.onCreate(savedInstanceState)
        analytics = Firebase.analytics
        auth= Firebase.auth
        db= Firebase.firestore
        enableEdgeToEdge()
        setContent {
            navControlller = rememberNavController()
            SinDesparchesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    Navegacion(navControlller, auth, db)
                }
            }
        }
    }
}

