package com.anafthdev.saku.ui.game

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.systemGesturesPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.anafthdev.saku.R
import com.anafthdev.saku.component.ObserveLifecycle
import com.anafthdev.saku.data.Difficulty
import com.anafthdev.saku.data.SakuDestination
import com.anafthdev.saku.extension.hourMinuteFormat
import com.anafthdev.saku.extension.label
import com.anafthdev.saku.extension.toast
import com.anafthdev.saku.uicomponent.AnimatedTextByChar
import com.anafthdev.saku.uicomponent.NumberPad
import com.anafthdev.saku.uicomponent.SakuDialog
import com.anafthdev.saku.uicomponent.SudokuBoard
import com.anafthdev.saku.uicomponent.SudokuGameAction
import com.anafthdev.saku.uicomponent.SudokuGameActionDefaults
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
	viewModel: GameViewModel,
	navController: NavController
) {
	
	val context = LocalContext.current
	
	LaunchedEffect(viewModel.win) {
		if (viewModel.win) {
			viewModel.saveState()
			viewModel.saveScore()
			"Win".toast(context)
		}
	}
	
	BackHandler {
		viewModel.saveState()
		viewModel.resetTimer()
		navController.popBackStack()
	}
	
	ObserveLifecycle(
		onResume = viewModel::resume,
		onPause = {
			viewModel.saveState()
			viewModel.pause()
		}
	)
	
	AnimatedVisibility(
		visible = viewModel.showFinishGameDialog,
		enter = fadeIn(
			animationSpec = tween(250)
		),
		exit = fadeOut(
			animationSpec = tween(250)
		)
	) {
		SakuDialog(
			onDismissRequest = {
				viewModel.resume()
				viewModel.updateShowFinishGameDialog(false)
			},
			text = {
				Text(
					text = stringResource(R.string.finish_game_msg),
					textAlign = TextAlign.Center
				)
			},
			confirmButton = {
				Button(
					onClick = {
						viewModel.resume()
						viewModel.updateShowFinishGameDialog(false)
						viewModel.validate()
					},
					modifier = Modifier
						.fillMaxWidth()
				) {
					Text(stringResource(R.string.yes))
				}
			},
			dismissButton = {
				TextButton(
					onClick = {
						viewModel.updateShowFinishGameDialog(false)
						viewModel.resume()
					},
					modifier = Modifier
						.fillMaxWidth()
				) {
					Text(stringResource(id = R.string.cancel))
				}
			}
		)
	}
	
	AnimatedVisibility(
		visible = viewModel.isPaused,
		enter = fadeIn(
			animationSpec = tween(250)
		),
		exit = fadeOut(
			animationSpec = tween(250)
		)
	) {
		SakuDialog(
			onDismissRequest = {
				viewModel.resume()
				viewModel.updateIsPaused(false)
			},
			text = {
				Text(
					text = stringResource(R.string.game_paused),
					textAlign = TextAlign.Center
				)
			},
			confirmButton = {
				Button(
					onClick = {
						viewModel.resume()
						viewModel.updateIsPaused(false)
					},
					modifier = Modifier
						.fillMaxWidth()
				) {
					Text(stringResource(R.string.resume))
				}
			},
			dismissButton = {
				TextButton(
					onClick = {
						viewModel.saveState()
						
						viewModel.updateIsPaused(false)
						
						navController.popBackStack()
					},
					modifier = Modifier
						.fillMaxWidth()
				) {
					Text(stringResource(id = R.string.exit))
				}
			}
		)
	}
	
	GameContent(
		viewModel = viewModel,
		onActionClicked = { action ->
			if (action in SudokuGameActionDefaults.selectableActions) {
				viewModel.updateSelectedGameAction(
					if (viewModel.selectedGameAction == action) SudokuGameAction.None
					else action
				)
			} else {
				when (action) {
					SudokuGameAction.Undo -> viewModel.undo()
					SudokuGameAction.Redo -> viewModel.redo()
					SudokuGameAction.Validate -> {
						viewModel.pause()
						viewModel.updateShowFinishGameDialog(true)
					}
					else -> {}
				}
			}
		},
		onPause = {
			viewModel.pause()
			viewModel.updateIsPaused(true)
		},
		onPrint = {
			val boardJson = Gson().toJson(viewModel.initialBoard)
			val solvedBoardJson = Gson().toJson(viewModel.solvedBoard)
			
			navController.navigate(
				SakuDestination.Sheet.Print.Home.createRoute(boardJson, solvedBoardJson)
			)
		},

		onBack = {
			viewModel.saveState()

			viewModel.updateIsPaused(false)

			navController.popBackStack()
		}
	)
}

@Composable
fun GameContent(
	viewModel: GameViewModel,
	onActionClicked: (SudokuGameAction) -> Unit,
	onPause: () -> Unit,
	onPrint: () -> Unit,
	onBack: () -> Unit
) {
	when(LocalConfiguration.current.orientation){
		Configuration.ORIENTATION_LANDSCAPE -> {
			//横屏布局
			LandscapeLayout(
				viewModel = viewModel,
				onActionClicked = onActionClicked,
				onPause = onPause,
				onPrint = onPrint,
				onBack = onBack
			)
		}
		//竖屏布局
		Configuration.ORIENTATION_PORTRAIT -> {
			PortraitLayout(
				viewModel = viewModel,
				onActionClicked = onActionClicked,
				onPause = onPause,
				onPrint = onPrint,
				onBack = onBack
			)
		}
	}

}

@Composable
fun PortraitLayout(
	viewModel: GameViewModel,
	onActionClicked: (SudokuGameAction) -> Unit,
	onPause: () -> Unit,
	onPrint: () -> Unit,
	onBack: () -> Unit,
){
	LazyColumn(
		verticalArrangement = Arrangement.SpaceBetween,
		contentPadding = PaddingValues(16.dp),
		modifier = Modifier
			.fillMaxSize()
			.systemBarsPadding()
	) {
		item {
			GameScreenHeader(
				minute = viewModel.minute,
				second = viewModel.second,
				hours = viewModel.hours,
				win = viewModel.win,
				difficulty = viewModel.difficulty,
				onPause = onPause,
				onPrint = onPrint,
				onBack = onBack
			)
		}
		
		item {
			SudokuBoard(
				cells = viewModel.board,
				win = viewModel.win,
				selectedCell = viewModel.selectedCell,
				onCellClicked = viewModel::updateBoard,
				highlightNumberEnabled = viewModel.highlightNumberEnabled,
				modifier = Modifier
					.fillMaxWidth()
					.aspectRatio(1f / 1f)
			)
		}
		
		item {
			Box(
				contentAlignment = Alignment.Center
			) {
				NumberPad(
					enabled = !viewModel.win,
					selectedNumber = viewModel.selectedNumber,
					remainingNumbers = viewModel.remainingNumbers,
					showRemainingNumber = viewModel.remainingNumberEnabled,
					onNumberSelected = viewModel::updateSelectedNumber,
					modifier = Modifier
						.fillMaxWidth()
				)
			}
		}
		
		item {
			Box(
				contentAlignment = Alignment.Center
			) {
				SudokuGameAction(
					enabled = !viewModel.win,
					selected = viewModel.selectedGameAction,
					onClick = onActionClicked,
					modifier = Modifier
						.fillMaxWidth()
				)
			}
		}
	}
}


@Composable
fun LandscapeLayout(
	viewModel: GameViewModel,
	onActionClicked: (SudokuGameAction) -> Unit,
	onPause: () -> Unit,
	onPrint: () -> Unit,
	onBack: () -> Unit,
) {
	Row(
		modifier = Modifier
			.systemBarsPadding()
			.systemGesturesPadding()
			.navigationBarsPadding()
			.fillMaxHeight()
	) {

		// Left Column for SudokuBoard
		Box(
			modifier = Modifier
				.fillMaxHeight()
				.padding(start = 16.dp)
		) {
			SudokuBoard(
				cells = viewModel.board,
				win = viewModel.win,
				selectedCell = viewModel.selectedCell,
				onCellClicked = viewModel::updateBoard,
				highlightNumberEnabled = viewModel.highlightNumberEnabled,
				modifier = Modifier
					.aspectRatio(1f / 1f)
			)
		}


		// Right Column for NumberPad and SudokuGameAction
		Column(
			modifier = Modifier
				.weight(1f)
		) {
			Box(
				contentAlignment = Alignment.Center,
				modifier = Modifier
			) {
				GameScreenHeader(
					minute = viewModel.minute,
					second = viewModel.second,
					hours = viewModel.hours,
					win = viewModel.win,
					difficulty = viewModel.difficulty,
					onPause = onPause,
					onPrint = onPrint,
					onBack = onBack,
					modifier = Modifier
						.fillMaxWidth()

				)
			}

			Box(
				contentAlignment = Alignment.Center,
				modifier = Modifier
					.weight(1f)
			) {
				NumberPad(
					enabled = !viewModel.win,
					selectedNumber = viewModel.selectedNumber,
					remainingNumbers = viewModel.remainingNumbers,
					showRemainingNumber = viewModel.remainingNumberEnabled,
					onNumberSelected = viewModel::updateSelectedNumber,
					modifier = Modifier
						.fillMaxWidth()
				)
			}

			Box(
				contentAlignment = Alignment.Center,
				modifier = Modifier
					.weight(1f)
			) {
				SudokuGameAction(
					enabled = !viewModel.win,
					selected = viewModel.selectedGameAction,
					onClick = onActionClicked,
					modifier = Modifier
						.fillMaxWidth()
				)
			}
		}
	}
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun GameScreenHeader(
	minute: Int,
	second: Int,
	hours: Int,
	win: Boolean,
	difficulty: Difficulty,
	modifier: Modifier = Modifier,
	onPause: () -> Unit,
	onPrint: () -> Unit,
	onBack: () -> Unit
) {
	
	Row(modifier = modifier
	) {
		AnimatedVisibility(
			visible = (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT),
			enter = expandHorizontally(
				animationSpec = tween(800)
			),
			exit = shrinkHorizontally(
				animationSpec = tween(800)
			) + scaleOut()
		) {
			IconButton(onClick = onBack) {
				Icon(
					painter = painterResource(id = R.drawable.ic_back),
					contentDescription = null
				)
			}
		}

		Spacer(modifier = Modifier.width(10.dp))

		Column {
			AnimatedTextByChar(
				text = "${hourMinuteFormat(hours)}:${hourMinuteFormat(minute)}:${hourMinuteFormat(second)}",
				style = MaterialTheme.typography.titleLarge.copy(
					fontWeight = FontWeight.ExtraBold
				)
			)
			
			Text(
				text = stringResource(id = difficulty.label),
				style = MaterialTheme.typography.titleLarge.copy(
					fontWeight = FontWeight.Light,
					color = Color.Gray
				)
			)
		}

		Spacer(modifier = Modifier.weight(1f))

		IconButton(onClick = onPrint) {
			Icon(
				painter = painterResource(id = R.drawable.ic_printer),
				contentDescription = null
			)
		}
		
		AnimatedVisibility(
			visible = !win,
			enter = expandHorizontally(
				animationSpec = tween(800)
			),
			exit = shrinkHorizontally(
				animationSpec = tween(800)
			) + scaleOut()
		) {
			IconButton(onClick = onPause) {
				Icon(
					painter = painterResource(id = R.drawable.ic_pause),
					contentDescription = null
				)
			}
		}
	}
}
