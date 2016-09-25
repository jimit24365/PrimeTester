package com.jimit24365.primetester.activity;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jimit24365.primetester.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class GameActivity extends AppCompatActivity {
    private static final Integer MIN_NUMBER = 1;
    private static final Integer MAX_NUMBER = 100;
    private static final Long GAME_TIME = Long.valueOf(120000);
    private static final String PLAYER_COUNT_STATE_KEY = "player_count_state";
    private static final String VIEW_MAP_STATE_KEY = "global_view_map_state";
    private static final String CHILD_TEXT_CONTAINER_STATE_KEY = "global_child_text_container_state";
    private static final String PRIME_RECORD_MAP_KEY = "prime_record";
    private static final String CURRENT_SCREEN_GAME_STATE_KEY = "current_screen_game_state";
    private static final String GAME_TIME_LEFT_STATE_KEY = "game_time_left_key";

    //Game state to change and react at any point of time in a
    enum GameState {
        STOPPED,
        PLAYING,
        PAUSED
    }

    Button conttroler;
    RelativeLayout balloon1;
    RelativeLayout balloon2;
    RelativeLayout balloon3;
    RelativeLayout balloon4;
    RelativeLayout balloon5;
    RelativeLayout screenCover;
    TextView scoreBoardTv;
    TextView screenCoverMessageTv;
    TextView gameTimerTv;

    int playerScore = 0;
    HashMap<String, Float> globalViewMap = new HashMap<>();
    HashMap<Integer, AnimatorSet> globalAnimatorMap = new HashMap<>(); // Map to store all active animations
    HashMap<Integer, Integer> globalChildTextContainer = new HashMap<>();
    HashMap<Integer, Boolean> primeRecorderMap = new HashMap<>();
    GameState currentScreenGameState = GameState.STOPPED;
    MediaPlayer balloonClickMediaPlayer;
    CountDownTimer gameCountDownTimer;
    Long gameTimeLeftYet = GAME_TIME;

    Integer currentGameLevel = 1;
    Interpolator currentInterpolator;
    Integer currentAnimationSpeed = 9000;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        balloonClickMediaPlayer = MediaPlayer.create(GameActivity.this, R.raw.pop);
        balloon1 = (RelativeLayout) findViewById(R.id.ballon_1);
        balloon2 = (RelativeLayout) findViewById(R.id.ballon_2);
        balloon3 = (RelativeLayout) findViewById(R.id.ballon_3);
        balloon4 = (RelativeLayout) findViewById(R.id.ballon_4);
        balloon5 = (RelativeLayout) findViewById(R.id.ballon_5);
        screenCover = (RelativeLayout) findViewById(R.id.screen_cover);
        screenCoverMessageTv = (TextView) screenCover.findViewById(R.id.screen_cover_message);
        scoreBoardTv = (TextView) findViewById(R.id.score_tv);
        conttroler = (Button) findViewById(R.id.controller_btn);
        gameTimerTv = (TextView) findViewById(R.id.game_timer_tv);

        // Handling Activity Recreation state
        if(savedInstanceState != null){
            extractSavedInstance(savedInstanceState);
            resetBalloonPosition(balloon1);
            resetBalloonPosition(balloon2);
            resetBalloonPosition(balloon3);
            resetBalloonPosition(balloon4);
            resetBalloonPosition(balloon5);
            scoreBoardTv.setText(playerScore+"");
        }else {
            setGlobalChildTextContainerId(balloon1);
            setGlobalChildTextContainerId(balloon2);
            setGlobalChildTextContainerId(balloon3);
            setGlobalChildTextContainerId(balloon4);
            setGlobalChildTextContainerId(balloon5);
        }

        // Handling Game Play/Pause button.
        conttroler.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (currentScreenGameState) {
                    case STOPPED:
                        startGame();
                        break;
                    case PAUSED:
                        resumeGame();
                        break;
                    case PLAYING:
                        pauseGame();
                        break;
                }
            }
        });

        //Handle ballon click events and playScore counts.
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                balloonClickMediaPlayer.start();
                processBalloonValue(v);
                if (v.getAnimation() != null) {
                    v.getAnimation().cancel();
                }
                v.clearAnimation();
                v.setVisibility(View.GONE);
                AnimatorSet currentObjectAnimator = globalAnimatorMap.get(v.getId());
                if (currentObjectAnimator != null) {
                    globalAnimatorMap.get(v.getId()).removeAllListeners();
                    globalAnimatorMap.get(v.getId()).end();
                    globalAnimatorMap.get(v.getId()).cancel();
                    animateBalloonView(v, false);
                }
            }
        };
        balloon1.setOnClickListener(onClickListener);
        balloon2.setOnClickListener(onClickListener);
        balloon3.setOnClickListener(onClickListener);
        balloon4.setOnClickListener(onClickListener);
        balloon5.setOnClickListener(onClickListener);
    }

    /*
    * Updating game clock
    * */
    private void gameTimerOnTick(long millisUntilFinished) {
        gameTimeLeftYet = millisUntilFinished;
        String currentTimeLeft = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisUntilFinished)), // The change is in this line
                TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)));
        gameTimerTv.setText(currentTimeLeft);
    }

    /*
    * handling game over situation
    * */
    private void gameTimerOnFinish() {
        if (currentScreenGameState == GameState.PLAYING) {
            stopGame();
        }
    }

    /*
    * Creating countDownTimer for time left (Also handing resuming state for clock)
    * */
    private CountDownTimer getGameTimer() {
        CountDownTimer countDownTimer;
        countDownTimer = new CountDownTimer(gameTimeLeftYet, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                gameTimerOnTick(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                gameTimerOnFinish();
            }
        };
        return countDownTimer;
    }

    /*
    * Starts game by setting all balloons position to initial stage.
    * */
    private void startGame() {
        gameCountDownTimer = getGameTimer();
        gameCountDownTimer.start();
        animateBalloonView(balloon1, true);
        animateBalloonView(balloon2, true);
        animateBalloonView(balloon3, true);
        animateBalloonView(balloon4, true);
        animateBalloonView(balloon5, true);
        currentScreenGameState = GameState.PLAYING;
        String controlButtonTitle = getResources().getString(R.string.pause_btn);
        conttroler.setText(controlButtonTitle);
        setCurrentScreenCover();
        invalidateOptionsMenu();
    }

    /*
    * Resumes game by checking all animators
    * */
    private void resumeGame() {

        gameCountDownTimer = getGameTimer();
        gameCountDownTimer.start();
        if(globalAnimatorMap != null && !globalAnimatorMap.isEmpty() && globalAnimatorMap.size() > 0)  {
            for (Map.Entry<Integer, AnimatorSet> entry : globalAnimatorMap.entrySet()) {
                AnimatorSet animatorSet = entry.getValue();
                if (animatorSet != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        if (animatorSet.isPaused()) {
                            animatorSet.resume();
                        }
                    } else {
                        animatorSet.start();
                    }
                }
            }
        }else{
            animateBalloonView(balloon1,false);
            animateBalloonView(balloon2,false);
            animateBalloonView(balloon3,false);
            animateBalloonView(balloon4,false);
            animateBalloonView(balloon5,false);
        }
        String controlButtonTitle = getResources().getString(R.string.pause_btn);
        conttroler.setText(controlButtonTitle);
        currentScreenGameState = GameState.PLAYING;
        setCurrentScreenCover();
        invalidateOptionsMenu();
    }

    private void pauseGame() {
        if (gameCountDownTimer != null) {
            gameCountDownTimer.cancel();
            gameCountDownTimer = null;
        }
        for (Map.Entry<Integer, AnimatorSet> entry : globalAnimatorMap.entrySet()) {
            AnimatorSet animatorSet = entry.getValue();
            if (animatorSet != null) {
                if (animatorSet.isRunning()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        animatorSet.pause();
                    } else {
                        animatorSet.cancel();
                    }
                }
            }
        }
        String controlButtonTitle = getResources().getString(R.string.play_btn);
        conttroler.setText(controlButtonTitle);
        currentScreenGameState = GameState.PAUSED;
        setCurrentScreenCover();
        invalidateOptionsMenu();
    }

    private void stopGame() {
        for (Map.Entry<Integer, AnimatorSet> entry : globalAnimatorMap.entrySet()) {
            AnimatorSet animatorSet = entry.getValue();
            if (animatorSet != null) {
                animatorSet.removeAllListeners();
                animatorSet.end();
                animatorSet = null;
            }
        }
        resetBalloonPosition(balloon1);
        resetBalloonPosition(balloon2);
        resetBalloonPosition(balloon3);
        resetBalloonPosition(balloon4);
        resetBalloonPosition(balloon5);
        String controlButtonTitle = getResources().getString(R.string.play_btn);
        conttroler.setText(controlButtonTitle);
        currentScreenGameState = GameState.STOPPED;
        setCurrentScreenCover();
        gameTimeLeftYet = GAME_TIME;
        invalidateOptionsMenu();
    }

    /*
    * This function will handle game cover screen state according to current game state.
    * */
    private void setCurrentScreenCover() {
        String screenCoverMessage = getResources().getString(R.string.start_game);
        ;
        switch (currentScreenGameState) {
            case STOPPED:
                screenCover.setVisibility(View.VISIBLE);
                break;
            case PLAYING:
                screenCover.setVisibility(View.INVISIBLE);
                break;
            case PAUSED:
                screenCover.setVisibility(View.VISIBLE);
                screenCoverMessage = getResources().getString(R.string.resume_game);
                break;
        }
        screenCoverMessageTv.setText(screenCoverMessage);
    }

    /*
    * This function will generate random number between 1 to 100 and assign to balloon view.
    * */
    private void setRandomBalloonNumber(View balloon) {
        int viewId = balloon.getId();
        TextView childPrimeTv = (TextView) findViewById(globalChildTextContainer.get(viewId));
        Random rand = new Random();
        int n = rand.nextInt(MAX_NUMBER) + MIN_NUMBER;
        childPrimeTv.setText(n + "");
    }

    /*
    * This function will fetch value assign to balloon view and add point to user if that value is
    * prime.
    * */
    private void processBalloonValue(View balloon){
        int viewId = balloon.getId();
        TextView childPrimeTv = (TextView) findViewById(globalChildTextContainer.get(viewId));
        Integer currentNumber = Integer.parseInt(childPrimeTv.getText().toString());
        if (isPrime(currentNumber)) {
            playerScore += 1;
            Animation animation = AnimationUtils.loadAnimation(GameActivity.this,R.anim.bounce);
            animation.setDuration(1000);
            scoreBoardTv.startAnimation(animation);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    scoreBoardTv.setText(playerScore + "");
                }

                @Override
                public void onAnimationEnd(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }
    }

    /*
    * Checks weather given number is prime or not and save for further references to reduce
    * recalculations.
    * */
    boolean isPrime(int n) {
        //check if n is a multiple of 2

        if(n == 2){
            primeRecorderMap.put(n, true);
            return true;
        }else if (n % 2 == 0){
            primeRecorderMap.put(n, false);
            return false;
        }
        //if not, then just check the odds
        if (primeRecorderMap.containsKey(n)) {
            if (primeRecorderMap.get(n)) {
                return true;
            } else {
                return false;
            }
        } else {
            for (int i = 3; i * i <= n; i += 2) {
                if (n % i == 0) {
                    primeRecorderMap.put(n, false);
                    return false;
                }
            }
            primeRecorderMap.put(n, true);
            return true;
        }
    }

    /*
    *   This is primary function to animate balloons in this game.
    * */
    private void animateBalloonView(final View balloon, boolean isFirstTime) {

        setRandomBalloonNumber(balloon);
        // Get the x, y coordinates of the screen center
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        // Get the starting coordinates of the view
        final int startPosition[] = new int[2];

        if (isFirstTime) {
            globalViewMap.put(balloon.getId() + "_x", balloon.getX());
            globalViewMap.put(balloon.getId() + "_y", balloon.getY());
        } else {

            resetBalloonPosition(balloon);
        }



        balloon.getLocationOnScreen(startPosition);
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator mover1 = ObjectAnimator.ofFloat(balloon, "translationY", (0 - displayMetrics.heightPixels));
        mover1.setTarget(balloon);
        mover1.setDuration(currentAnimationSpeed);
        mover1.setInterpolator(currentInterpolator);
        mover1.setRepeatCount(0);

        int toPoint = 0;
        if (startPosition[0] <= (displayMetrics.widthPixels / 2)) {
            toPoint = displayMetrics.widthPixels;
        } else {
            toPoint = 0 - displayMetrics.widthPixels;
        }
        ObjectAnimator mover2 = ObjectAnimator.ofFloat(balloon, "translationX", toPoint);
        mover2.setTarget(balloon);
        mover2.setDuration(currentAnimationSpeed);
        mover2.setRepeatCount(0);
        mover2.setInterpolator(currentInterpolator);
        animatorSet.playTogether(mover1, mover2);

        globalAnimatorMap.put(balloon.getId(), animatorSet);
        animatorSet.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animateBalloonView(balloon, false);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

        });
        animatorSet.start();
    }

    /*
    * This Function will reset all balloons positions to initial level
    * */
    private void resetBalloonPosition(View balloon){
        int viewId = balloon.getId();
        if(globalViewMap != null && globalViewMap.containsKey(viewId+"_x") && globalViewMap.containsKey(viewId+"_y")) {
            balloon.setX(globalViewMap.get(balloon.getId() + "_x"));
            balloon.setY(globalViewMap.get(balloon.getId() + "_y"));
            balloon.setVisibility(View.VISIBLE);
        }
        
    }

    /*
    * This Function will find and store balloon view's value container, Too reduce re-calculations
    * */
    private void setGlobalChildTextContainerId(View balloon) {
        int viewId = balloon.getId();
        String currentViewName = getResources().getResourceName(viewId);
        String currentViewNumber = currentViewName.charAt(currentViewName.length() - 1) + "";
        String childViewName = "ballon_" + currentViewNumber + "_value";
        int id = getResources().getIdentifier(childViewName, "id", GameActivity.this.getPackageName());
        globalChildTextContainer.put(viewId, id);
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentGameLevel = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(getResources().getString(R.string.preferenece_key),"1"));
        currentAnimationSpeed = 9000/currentGameLevel;
        switch (currentGameLevel){
            case 1:
                currentInterpolator = new LinearInterpolator();
                break;
            case 2:
                currentInterpolator = new AccelerateDecelerateInterpolator();
                break;
            case 3:
                currentInterpolator = new AccelerateInterpolator();
                break;
        }
        if (currentScreenGameState == GameState.PAUSED) {
            resumeGame();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(currentScreenGameState == GameState.PLAYING) {
            pauseGame();
        }
    }

    /*
    * Handling Activity destruction in case like destruction in background
    * */
    private void saveCurrentState(Bundle outState){
        outState.putInt(PLAYER_COUNT_STATE_KEY,playerScore);
        outState.putSerializable(VIEW_MAP_STATE_KEY,globalViewMap);
        outState.putSerializable(CHILD_TEXT_CONTAINER_STATE_KEY,globalChildTextContainer);
        outState.putSerializable(PRIME_RECORD_MAP_KEY,primeRecorderMap);
        outState.putSerializable(CURRENT_SCREEN_GAME_STATE_KEY,currentScreenGameState);
        outState.putLong(GAME_TIME_LEFT_STATE_KEY,gameTimeLeftYet);
    }

    private void extractSavedInstance(Bundle inState){
        playerScore = inState.getInt(PLAYER_COUNT_STATE_KEY);
        globalViewMap = (HashMap<String, Float>) inState.getSerializable(VIEW_MAP_STATE_KEY);
        globalChildTextContainer = (HashMap<Integer, Integer>) inState.getSerializable(CHILD_TEXT_CONTAINER_STATE_KEY);
        primeRecorderMap = (HashMap<Integer, Boolean>) inState.getSerializable(PRIME_RECORD_MAP_KEY);
        currentScreenGameState = (GameState) inState.getSerializable(CURRENT_SCREEN_GAME_STATE_KEY);
        gameTimeLeftYet = inState.getLong(GAME_TIME_LEFT_STATE_KEY);
    }

    /*
    * Enabling user to set game level according to his/her proficiency
    * */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings, menu);
        MenuItem settings = menu.findItem(R.id.settings);
        if(currentScreenGameState == GameState.STOPPED ){
            settings.setVisible(true);
        }else{
            settings.setVisible(false);
        }
        return  true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if(item.getItemId() == R.id.settings){
            Intent i = new Intent(GameActivity.this,SettingsActivity.class);
            startActivity(i);
        }
        return  true;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveCurrentState(outState);
    }

}
