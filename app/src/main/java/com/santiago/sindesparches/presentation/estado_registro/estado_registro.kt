package com.santiago.sindesparches.presentation.estado_registro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.santiago.sindesparches.R
import com.santiago.sindesparches.ui.theme.azul_comienzo
import com.santiago.sindesparches.ui.theme.azul_final
import com.santiago.sindesparches.ui.theme.azul_mitad
import com.santiago.sindesparches.ui.theme.boton_texto
import com.santiago.sindesparches.ui.theme.white

@Composable
fun estado_registro(navigateToperfil: () -> Unit = {} ,usuario:String){

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.succes))
    val progress by animateLottieCompositionAsState(composition)


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(azul_comienzo, azul_mitad, azul_final))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .width(280.dp)
                .height(350.dp)
                .background(color = boton_texto, shape = RoundedCornerShape(20.dp))
                .border(2.dp, color = white, shape = RoundedCornerShape(20.dp)),
            horizontalAlignment = Alignment.CenterHorizontally

        ){
            Spacer(modifier = Modifier.weight(0.2f))

            Text(
                text = "Bienvenido $usuario",
                color = white,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(150.dp)
            )
            Text(
                text = "Su registro fue exitoso, comenzaremos a configurar tu perfil, no te preocupes solo es un paso mas " +
                        "despues entra, disfruta y busca tu mejor plan y vive sin desparche!",
                color = white,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(
                    horizontal = 20.dp
                )
            )
            Spacer(modifier = Modifier.weight(0.2f))
        }

        Spacer(modifier = Modifier.weight(0.5f))

        Button(
            onClick = { navigateToperfil() },
            modifier = Modifier
                .width(150.dp)
                .height(50.dp)
        ) {
            Text(text = "aceptar")

        }
        Spacer(modifier = Modifier.weight(1f))

    }
}



