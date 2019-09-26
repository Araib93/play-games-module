package me.araib.module.play.games

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.*
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.games.Games
import com.google.android.gms.games.Player
import me.araib.core.BaseActivity

@SuppressLint("Registered")
class PlayGamesTraitImpl() : LifecycleObserver, PlayGamesTrait {
    private var context: Context? = null

    override fun initPlayGamesTrait(context: Context) {
        this.context = context
        (this.context as LifecycleOwner).lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy() {
        (context as? LifecycleOwner)?.lifecycle?.removeObserver(this)
        context = null
    }

    override val playGamesAvailabilityObservable = MutableLiveData<Boolean>().apply {
        value = false
    }

    override val googleAccountObservable = MutableLiveData<GoogleSignInAccount?>().apply {
        value = null
    }

    override val googlePlayerAccountObservable = MutableLiveData<Player?>().apply {
        value = null
    }

    private val PLAY_GAMES_AVAILABILITY_CODE = 1009
    private val PLAY_GAMES_SIGN_IN_CODE = 987
    private val RC_ACHIEVEMENT_UI = 9003
    private val RC_LEADERBOARD_UI = 9004
    private var mFirstRun = true

    override val mSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
            .requestProfile()
            .build()
    }
    override val mGoogleSignInClient by lazy {
        GoogleSignIn.getClient(context!!, mSignInOptions)
    }

    override val playGamesObserver = Observer<Boolean> {
        if (it) {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null) {
                handleSignOut(false)
            } else {
                if (GoogleSignIn.hasPermissions(account, *mSignInOptions.scopeArray)) {
                    handleSignIn(account)
                } else
                    handleSignOut(false)
            }
        } else {
            handleSignOut(true)
        }
    }

    override val googlePlayerObserver = Observer<Player?> {
        if (mFirstRun) {
            mFirstRun = false
            return@Observer
        }
        showSignedInToast(it)
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        playGamesAvailabilityObservable.removeObserver(playGamesObserver)
        googlePlayerAccountObservable.removeObserver(googlePlayerObserver)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        val googlePlayAvailability =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        if (googlePlayAvailability == ConnectionResult.SUCCESS) {
            playGamesAvailabilityObservable.value = true
        } else {
            if (playGamesAvailabilityObservable.value == true) {
                playGamesAvailabilityObservable.value = false
                GoogleApiAvailability
                    .getInstance()
                    .getErrorDialog(context as BaseActivity, googlePlayAvailability, PLAY_GAMES_AVAILABILITY_CODE)
                    .show()
            }
        }

        playGamesAvailabilityObservable.observe(context as BaseActivity, playGamesObserver)
        googlePlayerAccountObservable.observe(context as BaseActivity, googlePlayerObserver)
    }

    override fun handleResultPlayGames(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PLAY_GAMES_AVAILABILITY_CODE) {
            playGamesAvailabilityObservable.value = resultCode == Activity.RESULT_OK
        } else if (requestCode == PLAY_GAMES_SIGN_IN_CODE) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                handleSignIn(GoogleSignIn.getLastSignedInAccount(context))
            } else {
                handleSignOut(false)
                showPlayGamesSignInError(result.status.statusMessage)
            }
        }
    }

    override fun handleSignIn(account: GoogleSignInAccount?) {
        // Change states for sign in
        context?.let { context ->
            Games.getPlayersClient(context, account!!).currentPlayer.addOnSuccessListener {
                googleAccountObservable.value = account
                googlePlayerAccountObservable.value = it
                changeUIForSignIn(true)
            }.addOnFailureListener {
                handleSignOut(false)
            }
        }
    }

    override fun handleSignOut(error: Boolean) {
        // Change states for sign out
        googlePlayerAccountObservable.value = null
        googleAccountObservable.value = null
        changeUIForSignIn(if (error) null else false)
    }

    override fun toggleSignIn() {
        Log.e("PGAI: toggleSignIn", "Triggered")
        googleAccountObservable.value = GoogleSignIn.getLastSignedInAccount(context)
        if (GoogleSignIn.hasPermissions(
                googleAccountObservable.value,
                *mSignInOptions.scopeArray
            )
        ) {
            mGoogleSignInClient.signOut().addOnCompleteListener {
                changeUIForSignIn(!it.isSuccessful)
            }
        } else {
            (context as BaseActivity).startActivityForResult(mGoogleSignInClient.signInIntent, PLAY_GAMES_SIGN_IN_CODE)
        }
    }

    override fun signInToPlayGames() {
        (context as BaseActivity).startActivityForResult(mGoogleSignInClient.signInIntent, PLAY_GAMES_SIGN_IN_CODE)
    }

    override fun signOutFromPlayGames() {
        googleAccountObservable.value = GoogleSignIn.getLastSignedInAccount(context)
        if (GoogleSignIn.hasPermissions(
                googleAccountObservable.value,
                *mSignInOptions.scopeArray
            )
        ) {
            mGoogleSignInClient.signOut().addOnCompleteListener {
                changeUIForSignIn(!it.isSuccessful)
            }
        }
    }

    override fun unlockAchievement(
        @StringRes achievementId: Int,
        incrementBy: Int?
    ) {
        context?.let { context ->
            GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
                Games.getAchievementsClient(context, account).apply {
                    if (incrementBy == null) {
                        unlock(context.getString(achievementId))
                    } else {
                        increment(context.getString(achievementId), incrementBy)
                    }
                }
            }
        }
    }

    override fun showAchievements() {
        context?.let {context ->
            GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
                Games.getAchievementsClient(context, account)
                    .achievementsIntent
                    .addOnSuccessListener { intent ->
                        (context as BaseActivity).startActivityForResult(
                            intent,
                            RC_ACHIEVEMENT_UI
                        )
                    }
            }
        }
    }

    override fun submitScore(
        @StringRes leaderboardId: Int,
        score: Long
    ) {
        context?.let { context ->
            GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
                Games.getLeaderboardsClient(context, account)
                    .submitScore(context.getString(leaderboardId), score)
            }
        }
    }

    override fun showLeaderboard(@StringRes leaderboardId: Int) {
        context?.let { context ->
            Games.getLeaderboardsClient(context, GoogleSignIn.getLastSignedInAccount(context)!!)
                .getLeaderboardIntent(context.getString(leaderboardId))
                .addOnSuccessListener { intent -> (context as BaseActivity).startActivityForResult(intent, RC_LEADERBOARD_UI) }
        }
    }

    override fun showPlayGamesSignInError(message: String?) {
        // override as required
    }

    override fun showSignedInToast(player: Player?) {
        // override as required
    }

    override fun changeUIForSignIn(signedIn: Boolean?) {
        // override as required
    }
}