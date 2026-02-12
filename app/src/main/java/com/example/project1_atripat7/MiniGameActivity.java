package com.example.project1_atripat7;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.Arrays;
import java.util.Random;

public class MiniGameActivity extends AppCompatActivity {

    private static final String TAG = "YAHTZEE_MINI";

    private final int[] dice = new int[5];
    private final boolean[] categoryUsed = new boolean[6];

    private ToggleButton[] dieBtns;
    private Button btnRollAll, btnRollChosen, btnCommit;
    private TextView tvRollsLeft, tvScore;
    private RadioGroup rgCategories;

    private RadioButton rbOnes, rbTwos, rbThrees, rbFours, rbFives, rbSixes;

    private final Random rng = new Random();
    private int rollsLeft = 3;
    private int score = 0;
    private boolean hasRolledThisTurn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mini_game);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dieBtns = new ToggleButton[]{
                findViewById(R.id.die0),
                findViewById(R.id.die1),
                findViewById(R.id.die2),
                findViewById(R.id.die3),
                findViewById(R.id.die4)
        };

        // Listen for die selection changes
        for (ToggleButton b : dieBtns) {
            b.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateRollChosenEnabled();
            });
        }


        btnRollAll = findViewById(R.id.btnRollAll);
        btnRollChosen = findViewById(R.id.btnRollChosen);
        btnCommit = findViewById(R.id.btnCommit);
        tvRollsLeft = findViewById(R.id.tvRollsLeft);
        tvScore = findViewById(R.id.tvScore);
        rgCategories = findViewById(R.id.rgMiniCategories);




        rbOnes = findViewById(R.id.rbOnes);
        rbTwos = findViewById(R.id.rbTwos);
        rbThrees = findViewById(R.id.rbThrees);
        rbFours = findViewById(R.id.rbFours);
        rbFives = findViewById(R.id.rbFives);
        rbSixes = findViewById(R.id.rbSixes);

        startNewTurn(true);

        btnRollAll.setOnClickListener(v -> {
            Log.d(TAG, "Roll ALL clicked (rollsLeft=" + rollsLeft + ")");
            if (rollsLeft <= 0) return;

            boolean[] mask = new boolean[5];
            Arrays.fill(mask, true);
            rollDiceWithMask(mask);
        });

        btnRollChosen.setOnClickListener(v -> {
            Log.d(TAG, "Roll CHOSEN clicked (rollsLeft=" + rollsLeft + ")");
            if (rollsLeft <= 0) return;
            if (!anyDieSelected()) {
                Log.d(TAG, "Roll CHOSEN ignored (no dice selected).");
                return;
            }
            boolean[] mask = new boolean[5];
            for (int i = 0; i < 5; i++) mask[i] = dieBtns[i].isChecked();
            rollDiceWithMask(mask);
        });


        for (int i = 0; i < dieBtns.length; i++) {
            final int idx = i;

            dieBtns[i].setOnCheckedChangeListener((buttonView, isChecked) -> {
                Log.d(TAG, "Die toggled idx=" + idx + " checked=" + isChecked + " value=" + dice[idx]);
                updateRollChosenEnabled();
                int preview = previewSelectedCategoryPoints();
                btnCommit.setText("Choose (" + preview + " pts)");
            });

            GestureDetector detector = new GestureDetector(this,
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDoubleTap(MotionEvent e) {
                            if (rollsLeft <= 0) return true;
                            for (int j = 0; j < dieBtns.length; j++) {
                                dieBtns[j].setChecked(j == idx);
                            }
                            updateRollChosenEnabled();
                            int preview = previewSelectedCategoryPoints();
                            btnCommit.setText("Choose (" + preview + " pts)");
                            Log.d(TAG, "Die double-tapped -> exclusive select idx=" + idx);
                            return true;
                        }
                    });

            dieBtns[i].setOnTouchListener((v, event) -> detector.onTouchEvent(event));
        }

        btnCommit.setOnClickListener(v -> commitCategory());
    }

    private void startNewTurn(boolean freshGame) {
        hasRolledThisTurn = false;
        rollsLeft = 3;

        // 1. clear selected/held dice from previous turn
        for (ToggleButton b : dieBtns) {
            b.setChecked(false);
        }

        // 2. reset dice display
        if (freshGame) {
            Arrays.fill(dice, 1);
        }
        syncDiceImages();

        // 3. reset UI
        tvRollsLeft.setText("Rolls left: " + rollsLeft);
        btnCommit.setText("Choose (0 pts)");

        // 4. since nothing is selected yet, disable "roll chosen"
        updateRollChosenEnabled();



    }


    private void rollDice(boolean rollAll) {
        boolean[] mask = new boolean[5];
        Arrays.fill(mask, rollAll);
        rollDiceWithMask(mask);
    }

    private void rollDiceWithMask(boolean[] mask) {
        for (int i = 0; i < 5; i++) {
            if (mask[i]) dice[i] = rollDifferentFrom(dice[i]);

        }
        rollsLeft--;
        hasRolledThisTurn = true;
        syncDiceImages();
        tvRollsLeft.setText("Rolls left: " + rollsLeft);
    }

    private void commitCategory() {
        if (!hasRolledThisTurn) return;

        int id = rgCategories.getCheckedRadioButtonId();
        if (id == -1) return;

        int gained = previewSelectedCategoryPoints();
        score += gained;
        tvScore.setText("Score: " + score);

        RadioButton used = findViewById(id);
        if (used != null) used.setEnabled(false);

        markCategoryUsed(id);
        rgCategories.clearCheck();
        hasRolledThisTurn = false;
        endTurn();
         //  fully deselect the radio button

    }

    private void markCategoryUsed(int id) {
        if (id == R.id.rbOnes) categoryUsed[0] = true;
        else if (id == R.id.rbTwos) categoryUsed[1] = true;
        else if (id == R.id.rbThrees) categoryUsed[2] = true;
        else if (id == R.id.rbFours) categoryUsed[3] = true;
        else if (id == R.id.rbFives) categoryUsed[4] = true;
        else if (id == R.id.rbSixes) categoryUsed[5] = true;
    }

    private int previewSelectedCategoryPoints() {
        int id = rgCategories.getCheckedRadioButtonId();
        if (id == -1) return 0;

        if (id == R.id.rbOnes) return sumOfFace(1);
        if (id == R.id.rbTwos) return sumOfFace(2);
        if (id == R.id.rbThrees) return sumOfFace(3);
        if (id == R.id.rbFours) return sumOfFace(4);
        if (id == R.id.rbFives) return sumOfFace(5);
        if (id == R.id.rbSixes) return sumOfFace(6);
        return 0;
    }

    private int sumOfFace(int face) {
        int s = 0;
        for (int v : dice) if (v == face) s += face;
        return s;
    }

    private void endTurn() {
        boolean anyAvailable = false;
        for (boolean used : categoryUsed) if (!used) anyAvailable = true;

        if (!anyAvailable) {
            new AlertDialog.Builder(this)
                    .setTitle("Game Over 🎲")
                    .setMessage("Final Score: " + score + "\n\nPlay again?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", (d, w) -> resetGame())
                    .setNegativeButton("No", (d, w) -> finish())
                    .show();
        } else {
            startNewTurn(false);
        }
    }

    private void resetGame() {
        Log.d(TAG, "Reset game");
        score = 0;
        tvScore.setText("Score: 0");

        // Reset category usage state
        Arrays.fill(categoryUsed, false);

        // Re-enable all categories
        enableAllCategories(true);

        // Start a fresh new game
        startNewTurn(true);
    }


    private void enableAllCategories(boolean enable) {
        RadioButton[] all = {
                rbOnes, rbTwos, rbThrees, rbFours, rbFives, rbSixes
        };

        for (int i = 0; i < all.length; i++) {
            if (categoryUsed[i]) {
                all[i].setEnabled(false); // permanently used
            } else {
                all[i].setEnabled(enable); // only enable if still unused
            }
        }
    }



    private void syncDiceImages() {
        for (int i = 0; i < 5; i++) setDieBackgroundForValue(dieBtns[i], dice[i]);
    }

    private void setDieBackgroundForValue(ToggleButton btn, int value) {
        int resId = getResources().getIdentifier("die" + value + "_selector", "drawable", getPackageName());
        btn.setBackgroundResource(resId);
    }

    private boolean anyDieSelected() {
        for (ToggleButton b : dieBtns) if (b.isChecked()) return true;
        return false;
    }

    private void updateRollChosenEnabled() {
        btnRollChosen.setEnabled(rollsLeft > 0 && anyDieSelected());
    }

    private int rollDifferentFrom(int prev) {
        int v = rng.nextInt(6) + 1;
        while (v == prev) v = rng.nextInt(6) + 1;
        return v;
    }


}
