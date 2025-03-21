package com.santiago.sindesparches

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.santiago.sindesparches.presentation.estado_registro.estado_registro
import com.santiago.sindesparches.presentation.home.homeScreen
import com.santiago.sindesparches.presentation.inicio.InicialScreen
import com.santiago.sindesparches.presentation.loging.logingScreen
import com.santiago.sindesparches.presentation.perfil.PerfilScreen
import com.santiago.sindesparches.presentation.registro_completo.registro_completo


@Composable

fun Navegacion(navController: NavHostController,
               auth: FirebaseAuth,
                db: FirebaseFirestore) {

    NavHost(navController = navController, startDestination = "inicio") {
        composable("inicio") {
            InicialScreen(auth = auth,
                navigateToLoging = { navController.navigate("loging") },
                navigatehome = { navController.navigate("home") })
        }

        composable("loging") {
            logingScreen(auth = auth,
                navigatetoinicialScreen = { navController.navigate("inicio") },
                navigateToEstadoRegistro = { usuario -> navController.navigate("estado_registro/$usuario") })
        }

        composable("home") {
            homeScreen(auth = auth,
                navigateToInicial = {
                    navController.navigate("inicio") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }



        composable("estado_registro/{usuario}") { backStackEntry ->
            val usuario = backStackEntry.arguments?.getString("usuario") ?: ""

            estado_registro(
                auth = auth,
                usuario = usuario,
                navigateToperfil = {
                    navController.navigate("perfil") {
                        popUpTo("loging") {
                            inclusive = true
                        }
                    }

                },
                onLogout = {
                    navController.navigate("inicio") {
                        popUpTo("estado_registro") {
                            inclusive = true
                        }
                    }
                }
            )

        }

        composable("perfil") {
            PerfilScreen(auth = auth, db,
            navigate_registro_completo= {nombre->
                navController.navigate("registro_completo/$nombre"){
                    popUpTo("estado_registro"){
                        inclusive = true
                    }
                }
            },onLogout = {navController.navigate("inicio"){
                    popUpTo("perfil"){
                        inclusive = true
                    }
                }
                })

        }

        composable("registro_completo/{nombre}") { backStackEntry ->
            val nombre = backStackEntry.arguments?.getString("nombre") ?: ""
            registro_completo(
                auth = auth,
                nombre = nombre,
                navigateToHome = {
                    navController.navigate("home") {
                        popUpTo("perfil") {
                            inclusive = true
                        }
                    }
                }
                ,
                navigateToLoging = {
                    navController.navigate("inicio") {
                        popUpTo("registro_completo") {
                            inclusive = true
                        }
                    }
                }

            )

        }

    }
}


