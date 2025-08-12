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
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.santiago.sindesparches.presentation.location_settings.LocationSettingsScreen
import com.santiago.sindesparches.presentation.verificacion.VerificacionCorreoScreen
import kotlinx.coroutines.delay


@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun Navegacion(
    navController: NavHostController,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    intent: Intent
) {
    var notificationData by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    // Detectar cambios en el intent
    LaunchedEffect(intent) {
        val planId = intent.getStringExtra("planId")
        val fromNotification = intent.getBooleanExtra("fromNotification", false)

        Log.d("Navegacion", "Procesando intent - planId: $planId, fromNotification: $fromNotification")

        if (fromNotification && planId != null) {
            notificationData = Pair(planId, true)
        }
    }

    // Manejar la navegación cuando tengamos los datos
    LaunchedEffect(notificationData, navController) {
        notificationData?.let { (planId, shouldNavigate) ->
            if (shouldNavigate) {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    Log.d("Navegacion", "🚀 Iniciando navegación a plan: $planId")

                    // Limpiar el estado para evitar navegaciones repetidas
                    notificationData = Pair(planId, false)

                    try {
                        // Esperar a que el NavController esté listo
                        var attempts = 0
                        while (navController.currentDestination == null && attempts < 10) {
                            delay(200)
                            attempts++
                        }

                        val currentRoute = navController.currentDestination?.route
                        Log.d("Navegacion", "Ruta actual después de espera: $currentRoute")

                        // Asegurar que estamos en una ruta base válida
                        if (currentRoute == "inicio" || currentRoute == null) {
                            navController.navigate("home") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                            }
                            delay(800) // Tiempo para completar la navegación
                        }

                        // Navegar al detalle del plan
                        navController.navigate("plan_detail/$planId") {
                            launchSingleTop = true
                        }

                        Log.d("Navegacion", "✅ Navegación exitosa a plan_detail/$planId")

                    } catch (e: Exception) {
                        Log.e("Navegacion", "❌ Error en navegación desde notificación", e)
                        // Reintentar una vez
                        delay(1000)
                        try {
                            navController.navigate("plan_detail/$planId")
                        } catch (retryException: Exception) {
                            Log.e("Navegacion", "❌ Fallo en reintento de navegación", retryException)
                        }
                    }
                } else {
                    Log.w("Navegacion", "⚠️ Usuario no autenticado para navegar")
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = "inicio") {

        composable("inicio") {
            InicialScreen(navController, auth, db, navigatehome = {
                navController.navigate("home")
            }, navigatePerfil = {
                navController.navigate("perfil/{usuario}")
            }, navigateToLoging = { navController.navigate("loging") },
                navigateToVerificacionCorreo = { email, usuario ->
                    navController.navigate("verificacion/$email/$usuario")
                })
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
            logingScreen(
                auth = auth,
                navigatetoinicialScreen = { navController.navigate("inicio") },
                navigateToVerificacionCorreo = { correo, usuario ->
                    navController.navigate("verificacion/$correo/$usuario")
                }
            )
        }

        composable(
            route = "verificacion/{correo}/{usuario}",
            arguments = listOf(
                navArgument("correo") { type = NavType.StringType },
                navArgument("usuario") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val correo = backStackEntry.arguments?.getString("correo") ?: ""
            val usuario = backStackEntry.arguments?.getString("usuario") ?: ""

            VerificacionCorreoScreen(
                correo = correo,
                usuario = usuario,
                auth = auth,
                navigateToEstadoRegistro = { usuario ->
                    navController.navigate("estado_registro/$usuario") {
                        popUpTo("loging") { inclusive = true }
                    }
                },
                navigateToLoging = {
                    navController.navigate("loging") {
                        popUpTo("verificacion_correo") { inclusive = true }
                    }
                }
            )
        }
// ✅ NAVEGACIÓN HOME ACTUALIZADA
        composable("home") {
            homeScreen(
                auth = auth,
                db = db,
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
                },
                navigateToParticipar = {
                    navController.navigate("participar")
                },
                navigateToNotifications = {
                    navController.navigate("notifications")
                },
                navigateToComments = { planId ->
                    navController.navigate("comments/$planId")
                },
                // ✅ NUEVA NAVEGACIÓN PARA CONFIGURAR UBICACIÓN
                navigateToLocationSettings = {
                    navController.navigate("location_settings")
                }
            )
        }


        composable("location_settings") {
            LocationSettingsScreen(
                navController = navController,
                onLocationSelected = { location ->
                    // Opcional: hacer algo adicional cuando se seleccione ubicación
                    Log.d("Navigation", "Ubicación seleccionada: ${location.displayName}")
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
                }
            )
        }
        composable("publicaciones") {
            publicacion_screen(
                auth = auth,
                db = db,
                navController = navController,
                navigateToHome = {
                    navController.navigate("home")
                },
                navigateToMapPicker = {
                    navController.navigate("map_picker") {
                        launchSingleTop = true
                        // No usar popUpTo aquí para preservar el back stack
                    }
                }
            )
        }

        composable("map_picker") {
            com.santiago.sindesparches.presentation.map_picker.MapPickerScreen(
                navController = navController
            )
        }

        composable(
            route = "editplanscreen/{planId}",
            arguments = listOf(
                navArgument("planId") {
                    type = NavType.StringType
                    nullable = false
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getString("planId")

            PublicacionScreen(
                auth = auth,
                db = db,
                navigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                planId = if (planId.isNullOrBlank()) null else planId,
                navigateToMapPicker = {
                    navController.navigate("map_picker") {
                        launchSingleTop = true
                        // No usar popUpTo aquí para preservar el back stack
                    }
                },
                navController = navController
            )
        }

        composable(
            route = "plan_detail/{planId}",
            arguments = listOf(
                navArgument("planId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getString("planId") ?: ""

            if (planId.isBlank()) {
                Log.e("Navigation", "PlanId está vacío en plan_detail")
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
                return@composable
            }

            Log.d("Navigation", "Mostrando detalle del plan: $planId")

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
                }
            )
        }

        composable(
            route = "perfilusuario/{userId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""

            if (userId.isBlank()) {
                Log.e("Navigation", "UserId está vacío en perfilusuario")
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
                },
                navigateToComments = { planId ->
                    navController.navigate("comments/$planId")
                }
            )
        }

        composable("mi_perfil") {
            MiPerfilScreen(
                auth = auth,
                db = db,
                navigatehome = { navController.navigate("home") },
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
                },
                navigateToComments = { planId ->
                    navController.navigate("comments/$planId")
                },
                navigateToEditPlan = { planId ->
                    navController.navigate("editplanscreen/$planId")
                }
            )
        }

        composable("megusta") {
            megustascreen(
                auth = auth,
                db = db,
                navigatehome = { navController.navigate("home") },
                navigateToUserProfile = { userID ->
                    navController.navigate("perfilusuario/$userID")
                },
                navigateToDetail_Plan = { planId ->
                    navController.navigate("plan_detail/$planId")
                },
                navigateToMiPerfil = {
                    navController.navigate("mi_perfil")
                },
                navigateToComments = { planId ->
                    navController.navigate("comments/$planId")
                }
            )
        }

        composable("participar") {
            planesParticipoScreen(auth = auth,
                db = db,
                navigatehome = { navController.navigate("home") },
                navigateToUserProfile = { userID ->
                    navController.navigate("perfilusuario/$userID")
                },
                navigateToDetail_Plan = { planId ->
                    navController.navigate("plan_detail/$planId")
                },
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
            val context = LocalContext.current
            CommentsScreen(
                planId = planId,
                db = db,
                auth = auth,
                context = context,
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
