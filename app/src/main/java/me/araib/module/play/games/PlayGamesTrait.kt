package me.araib.module.play.games

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.games.Player
import me.araib.core.utils.*

@ExposedClass(
    author = "araib.shafiq@ideofuzion.com",
    purpose = "Base class extension for easy handling of play games account and states",
    provides = [
        "toggleSignIn",
        "signInToPlayGames",
        "signOutFromPlayGames",
        "unlockAchievement",
        "showAchievements",
        "submitScore",
        "showLeaderboard",
        "playGamesAvailabilityObservable",
        "googleAccountObservable",
        "googlePlayerAccountObservable"
    ],
    requires = [
        "showPlayGamesSignInError",
        "changeUIForSignIn",
        "showSignedInToast"
    ]
)
interface PlayGamesTrait {
    fun initPlayGamesTrait(context: Context)

    @ExposedProperty(purpose = "Observable for checking play games availability manually")
    @PossibleValue(
        values = [
            "true -> Google play services found",
            "false -> Google play services not found"]
    )
    val playGamesAvailabilityObservable: MutableLiveData<Boolean>

    @ExposedProperty(purpose = "Observable for Google account")
    @PossibleValue(
        values = [
            "<GoogleSignInAccount> -> Account found",
            "null -> No account found"
        ]
    )
    val googleAccountObservable: MutableLiveData<GoogleSignInAccount?>

    @ExposedProperty(purpose = "Observable for Google play games account")
    @PossibleValue(
        values = [
            "<Player> -> Account found",
            "null -> No account found"
        ]
    )
    val googlePlayerAccountObservable: MutableLiveData<Player?>

    val mSignInOptions: GoogleSignInOptions
    val mGoogleSignInClient: GoogleSignInClient

    val playGamesObserver: Observer<Boolean>
    val googlePlayerObserver: Observer<Player?>

    fun handleResultPlayGames(requestCode: Int, resultCode: Int, data: Intent?)

    fun handleSignIn(account: GoogleSignInAccount? = null)
    fun handleSignOut(error: Boolean = false)

    @ExposedProvideFunction(purpose = "For sign in and sign out from play games service")
    fun toggleSignIn()

    @ExposedProvideFunction(purpose = "For sign in to play games service")
    fun signInToPlayGames()

    @ExposedProvideFunction(purpose = "For sign out from play games service")
    fun signOutFromPlayGames()

    @ExposedProvideFunction(purpose = "For unlocking or incrementing achievement")
    @PossibleValues(
        name = "achievementId",
        values = ["Int -> Res id of achievement id string"]
    )
    @PossibleValues(
        name = "incrementBy",
        values = [
            "null -> Unlock achievement",
            "Int -> Increment achievement"
        ]
    )
    fun unlockAchievement(
        @StringRes achievementId: Int,
        incrementBy: Int? = null
    )

    @ExposedProvideFunction(purpose = "For launching achievement activity")
    fun showAchievements()

    @ExposedProvideFunction(purpose = "For submitting score on leaderboard")
    @PossibleValues(
        name = "leaderboardId",
        values = ["Int -> Res id of leaderboard id string"]
    )
    @PossibleValues(
        name = "score",
        values = ["Long -> score obtained by user"]
    )
    fun submitScore(
        @StringRes leaderboardId: Int,
        score: Long
    )

    @ExposedProvideFunction(purpose = "For launching leaderboard activity for the given id")
    @PossibleValues(
        name = "leaderboardId",
        values = ["Int -> Res id of leaderboard id string"]
    )
    fun showLeaderboard(@StringRes leaderboardId: Int)

    @ExposedImplementFunction(purpose = "For showing error dialog if need be")
    @PossibleValues(
        name = "message",
        values = [
            "<String> -> Sign in error message",
            "null -> No error message found. Use customized message"
        ]
    )
    fun showPlayGamesSignInError(message: String?)

    fun changeUIForSignIn(signedIn: Boolean?)
    fun showSignedInToast(player: Player?)
}