package com.santiago.sindesparches

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.santiago.sindesparches.presentation.contraseña.DefinirContraseñaScreen
import com.santiago.sindesparches.presentation.editplanscreen.PublicacionScreen
import com.santiago.sindesparches.presentation.estado_registro.estado_registro
import com.santiago.sindesparches.presentation.flash_plan.flash_plan
import com.santiago.sindesparches.presentation.home.homeScreen
import com.santiago.sindesparches.presentation.inicio.InicialScreen
import com.santiago.sindesparches.presentation.loging.logingScreen
import com.santiago.sindesparches.presentation.megusta.megustascreen
import com.santiago.sindesparches.presentation.mi_perfil.MiPerfilScreen
import com.santiago.sindesparches.presentation.participar.planesParticipoScreen
import com.santiago.sindesparches.presentation.perfil.PerfilScreen
import com.santiago.sindesparches.presentation.perfilusuario.UserProfileScreen
import com.santiago.sindesparches.presentation.plan_detail.PlanDetailScreen
import com.santiago.sindesparches.presentation.publicaciones.publicacion_screen
import com.santiago.sindesparches.presentation.registro_completo.registro_completo
import com.santiago.sindesparches.presentation.notifications.NotificationsScreen
import com.santiago.sindesparches.presentation.comments.CommentsScreen


@RequiresApi(Build.VERSION_CODES.S)
@Composable

fun Navegacion(navController: NavHostController,
               auth: FirebaseAuth,
                db: FirebaseFirestore) {

    NavHost(navController = navController, startDestination = "inicio") {

        composable("inicio") {
            InicialScreen(navController, auth, db, navigatehome = {
                navController.navigate("home")
            }, navigatePerfil = {
                navController.navigate("perfil/{usuario}")
            }, navigateToLoging = { navController.navigate("loging") })

        }

        composable("definir_contrasena/{email}") { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            DefinirContraseñaScreen(email = email, auth = auth, onLogout = {
                navController.navigate("inicio") {
                    popUpTo("estado_registro") {
                        inclusive = true
                    }
                }
            }) { navController.navigate("estado_registro/$email") }
        }

        composable("loging") {
            logingScreen(auth = auth,
                navigatetoinicialScreen = { navController.navigate("inicio") },
                navigateToEstadoRegistro = { usuario -> navController.navigate("estado_registro/$usuario") })
        }

        composable("home") {

            homeScreen(auth = auth, db,
                navigateToInicial = {
                    navController.navigate("inicio") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                navigateToFlashPlan = {
                    navController.navigate("flash_plan") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                navigateToPublicaciones = {
                    navController.navigate("publicaciones")
                },
                navigateToPlanDetail = { planId ->
                    navController.navigate("plan_detail/$planId")
                },
                navigateToUserProfile = { userID ->
                    navController.navigate("perfilusuario/$userID")
                },
                navigateToMiPerfil = {
                    navController.navigate("mi_perfil")
                },
                navigateToEditPlan = { planId ->
                    navController.navigate("editplanscreen/$planId")
                },
                navigateToMegusta = {
                    navController.navigate("megusta")
                }
                ,
                navigateToParticipar = {
                    navController.navigate("participar")
                },
                navigateToNotifications = {
                    navController.navigate("notifications")
                },
                navigateToComments = { planId ->
                    navController.navigate("comments/$planId")
                }
            )
        }

        composable("flash_plan") {
            flash_plan(auth = auth, db,
                navigateToHome = {
                    navController.navigate("home")
                }
            )
        }

        composable("estado_registro/{usuario}") { backStackEntry ->
            val usuario = backStackEntry.arguments?.getString("usuario") ?: ""

            estado_registro(
                auth = auth,
                usuario = usuario,
                navigateToperfil = {
                    navController.navigate("perfil/$usuario") {
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

        composable("perfil/{usuario}") { backStackEntry ->
            val usuario = backStackEntry.arguments?.getString("usuario") ?: ""

            PerfilScreen(usuario = usuario, auth = auth, db,
                navigate_registro_completo = { nombre ->
                    navController.navigate("registro_completo/$nombre") {
                        popUpTo("estado_registro") {
                            inclusive = true
                        }
                    }
                }, onLogout = {
                    navController.navigate("inicio") {
                        popUpTo("perfil") {
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
                },
                navigateToLoging = {
                    navController.navigate("inicio") {
                        popUpTo("registro_completo") {
                            inclusive = true
                        }
                    }
                },
                navigateToComments = { planId ->
                    navController.navigate("comments/$planId")
                }
            )
        }

        composable("publicaciones") {
            publicacion_screen(auth = auth, db,
                navigateToHome = {
                    navController.navigate("home")
                }
            )
        }

        composable(
            route = "editplanscreen/{planId}",
            arguments = listOf(
                navArgument("planId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getString("planId") ?: ""
            PublicacionScreen(
                auth = auth,
                db = db,
                navigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                planId = planId // Pasamos el ID del plan para indicar que estamos en modo edición
            )
        }


        composable(
            route = "plan_detail/{planId}",
            arguments = listOf(
                navArgument("planId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getString("planId") ?: ""
            PlanDetailScreen(
                planId = planId,
                auth = auth,
                db = db,
                navigateBack = {
                    navController.popBackStack()
                },
                navigateToEdit = { planToEditId ->
                    navController.navigate("editplanscreen/$planToEditId")
                },
                navigateToUserProfile = { userID ->
                    navController.navigate("perfilusuario/$userID")
                },

            )
        }
        composable(
            route = "perfilusuario/{userId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""

            // ✅ VALIDAR que userId no esté vacío
            if (userId.isBlank()) {
                Log.e("Navigation", "UserId está vacío en perfilusuario")
                // Opcionalmente navegar hacia atrás o mostrar error
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
                return@composable
            }

            Log.d("Navigation", "Navegando a perfil de usuario: $userId")

            UserProfileScreen(
                userId = userId,
                auth = auth,
                db = db,
                navigateToPlanDetail = { planId ->
                    if (planId.isNotBlank()) {
                        Log.d("Navigation", "Navegando a plan detail: $planId")
                        navController.navigate("plan_detail/$planId")
                    } else {
                        Log.e("Navigation", "PlanId está vacío")
                    }
                },
                navigateBack = {
                    navController.popBackStack()
                },
                currentUserId = auth.currentUser?.uid ?: "",
                navigateToProfile = { userID ->
                    navController.navigate("perfilusuario/$userID")
                },
                navigateToMiPerfil = {
                    navController.navigate("mi_perfil")
                }
            )
        }

        composable( "mi_perfil"){
            MiPerfilScreen(
                auth=auth,
                db=db,
                navigatehome ={navController.navigate("home")},
                navigateToPlanDetail = { planId ->
                    navController.navigate("plan_detail/$planId")
                },
                navigateToProfile = { userID ->
                    navController.navigate("perfilusuario/$userID")
                },
                navigateToMiPerfil = {
                    navController.navigate("mi_perfil")
                },
                onLogout = {
                    navController.navigate("inicio") {
                        popUpTo("mi_perfil") {
                            inclusive = true
                        }
                    }
                }

            )

        }

        composable("megusta"){
            megustascreen(
                auth=auth,
                db=db,
                navigatehome ={navController.navigate("home",)},
                navigateToUserProfile = { userID ->
                    navController.navigate("perfilusuario/$userID")
                },
                navigateToDetail_Plan = { planId ->
                    navController.navigate("plan_detail/$planId")},
                navigateToMiPerfil = {
                    navController.navigate("mi_perfil")
                },
                navigateToComments = { planId ->
                    navController.navigate("comments/$planId")
                }
            )
        }

        composable ("participar"){
            planesParticipoScreen(auth=auth,
                db=db,
                navigatehome ={navController.navigate("home",)},
                navigateToUserProfile = { userID ->
                    navController.navigate("perfilusuario/$userID")
                },
                navigateToDetail_Plan = { planId ->
                    navController.navigate("plan_detail/$planId")},
                navigateToMiPerfil = {
                    navController.navigate("mi_perfil")
                },
                navigateToComments = { planId ->
                    navController.navigate("comments/$planId")
                }
            )
        }

        composable("notifications") {
            NotificationsScreen(
                db = db,
                auth = auth,
                navigateToPlanDetail = { planId ->
                    navController.navigate("plan_detail/$planId")
                },
                navigateToUserProfile = { userId ->
                    navController.navigate("perfilusuario/$userId")
                },
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "comments/{planId}",
            arguments = listOf(
                navArgument("planId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getString("planId") ?: ""
            CommentsScreen(
                planId = planId,
                db = db,
                auth = auth,
                navigateBack = {
                    navController.popBackStack()
                },
                navigateToUserProfile = { userId ->
                    navController.navigate("perfilusuario/$userId")
                }
            )
        }


    }
}


