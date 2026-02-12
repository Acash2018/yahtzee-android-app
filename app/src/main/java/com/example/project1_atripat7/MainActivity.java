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

public class MainActivity extends AppCompatActivity {
    private final boolean[] categoryUsed = new boolean[13]; // 13 Yahtzee categories

    private static final String TAG = "YAHTZEE";

    // --- Dice state ---
    private final int[] dice = new int[5]; // values 1..6
    private ToggleButton[] dieBtns;
    private final Random rng = new Random();
    private int rollsLeft = 3;

    // --- UI ---
    private Button btnRollAll, btnRollChosen, btnCommit;
    private TextView tvRollsLeft, tvScore;
    private RadioGroup rgCategories;

    // --- Scoring / categories ---
    private int score = 0;
    private RadioButton rbOnes, rbTwos, rbThrees, rbFours, rbFives, rbSixes, rbYahtzee;
    private RadioButton rbChance, rbThreeKind, rbFourKind;
    private RadioButton rbFullHouse, rbSmallStraight, rbLargeStraight;

    private boolean hasRolledThisTurn = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- Hook up UI elements ---
        dieBtns = new ToggleButton[]{
                findViewById(R.id.die0),
                findViewById(R.id.die1),
                findViewById(R.id.die2),
                findViewById(R.id.die3),
                findViewById(R.id.die4)
        };

        btnRollAll    = findViewById(R.id.btnRollAll);
        btnRollChosen = findViewById(R.id.btnRollChosen);
        btnCommit     = findViewById(R.id.btnCommit);
        tvRollsLeft   = findViewById(R.id.tvRollsLeft);
        tvScore       = findViewById(R.id.tvScore);

        // Two separate RadioGroups for categories (left & right columns)
        RadioGroup rgColumnLeft = findViewById(R.id.rgColumnLeft);
        RadioGroup rgColumnRight = findViewById(R.id.rgColumnRight);

        // Hook up all category buttons
        rbOnes   = findViewById(R.id.rbOnes);
        rbTwos   = findViewById(R.id.rbTwos);
        rbThrees = findViewById(R.id.rbThrees);
        rbFours  = findViewById(R.id.rbFours);
        rbFives  = findViewById(R.id.rbFives);
        rbSixes  = findViewById(R.id.rbSixes);
        rbYahtzee = findViewById(R.id.rbYahtzee);

        rbChance        = findViewById(R.id.rbChance);
        rbThreeKind     = findViewById(R.id.rbThreeKind);
        rbFourKind      = findViewById(R.id.rbFourKind);
        rbFullHouse     = findViewById(R.id.rbFullHouse);
        rbSmallStraight = findViewById(R.id.rbSmallStraight);
        rbLargeStraight = findViewById(R.id.rbLargeStraight);

        // Start game
        startNewTurn(/*freshGame=*/true);

        // --- Button Listeners ---
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

        // --- Dice Toggle / Gesture Listeners ---
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

        // --- Two-column category behavior ---
        final RadioButton[] lastChecked = {null};

        final RadioGroup.OnCheckedChangeListener[] syncGroups = new RadioGroup.OnCheckedChangeListener[1];

        syncGroups[0] = (group, checkedId) -> {
            if (checkedId == -1) return;
            if (!hasRolledThisTurn) {
                // Prevent category selection before rolling
                //group.clearCheck();
                Log.d(TAG, "Ignored category selection before rolling");
                return;
            }

            RadioButton newlyChecked = findViewById(checkedId);

            // Deselect if same button tapped again
            if (lastChecked[0] == newlyChecked && newlyChecked != null) {
                // Temporarily disable listeners to avoid recursive crashes
                rgColumnLeft.setOnCheckedChangeListener(null);
                rgColumnRight.setOnCheckedChangeListener(null);

                rgColumnLeft.clearCheck();
                rgColumnRight.clearCheck();

                lastChecked[0] = null;
                btnCommit.setText("Choose (0 pts)");

                // Reattach listeners
                rgColumnLeft.setOnCheckedChangeListener(syncGroups[0]);
                rgColumnRight.setOnCheckedChangeListener(syncGroups[0]);
                return;
            }


            // Make sure only one is selected across both columns
            if (checkedId != -1) {
                if (group == rgColumnLeft) {
                    rgColumnRight.setOnCheckedChangeListener(null);
                    rgColumnRight.clearCheck();
                    rgColumnRight.setOnCheckedChangeListener(syncGroups[0]);
                } else {
                    rgColumnLeft.setOnCheckedChangeListener(null);
                    rgColumnLeft.clearCheck();
                    rgColumnLeft.setOnCheckedChangeListener(syncGroups[0]);
                }

                lastChecked[0] = newlyChecked;
                int preview = previewSelectedCategoryPoints();
                btnCommit.setText("Choose (" + preview + " pts)");
            }
        };

        rgColumnLeft.setOnCheckedChangeListener(syncGroups[0]);
        rgColumnRight.setOnCheckedChangeListener(syncGroups[0]);

        // --- Commit Button ---
        btnCommit.setOnClickListener(v -> {
            if (!hasRolledThisTurn) return;

            int id = rgColumnLeft.getCheckedRadioButtonId();
            if (id == -1) id = rgColumnRight.getCheckedRadioButtonId();
            if (id == -1) return;

            // Apply score before UI state changes
            int gained = previewSelectedCategoryPoints();
            score += gained;
            tvScore.setText("Score: " + score);
            Log.d(TAG, "Committed category: +" + gained + " pts (total=" + score + ")");

            // Disable only the selected category
            RadioButton used = findViewById(id);
            if (used != null) {
                if (used.isChecked()) {
                    used.setOnCheckedChangeListener(null);
                    used.setChecked(false);
                }
                used.setEnabled(false);
            }

            hasRolledThisTurn = false;
            disableUsedCategories(); // only keep used ones disabled
            markCategoryUsed(id);


            // Clear both groups safely
            try {
                rgColumnLeft.setOnCheckedChangeListener(null);
                rgColumnRight.setOnCheckedChangeListener(null);
                rgColumnLeft.clearCheck();
                rgColumnRight.clearCheck();
            } finally {
                rgColumnLeft.setOnCheckedChangeListener(syncGroups[0]);
                rgColumnRight.setOnCheckedChangeListener(syncGroups[0]);
            }

            btnCommit.setText("Choose (0 pts)");
            endTurn();
        });




    }

    private void markCategoryUsed(int id) {
        if (id == R.id.rbOnes) categoryUsed[0] = true;
        else if (id == R.id.rbTwos) categoryUsed[1] = true;
        else if (id == R.id.rbThrees) categoryUsed[2] = true;
        else if (id == R.id.rbFours) categoryUsed[3] = true;
        else if (id == R.id.rbFives) categoryUsed[4] = true;
        else if (id == R.id.rbSixes) categoryUsed[5] = true;
        else if (id == R.id.rbThreeKind) categoryUsed[6] = true;
        else if (id == R.id.rbFourKind) categoryUsed[7] = true;
        else if (id == R.id.rbFullHouse) categoryUsed[8] = true;
        else if (id == R.id.rbSmallStraight) categoryUsed[9] = true;
        else if (id == R.id.rbLargeStraight) categoryUsed[10] = true;
        else if (id == R.id.rbYahtzee) categoryUsed[11] = true;
        else if (id == R.id.rbChance) categoryUsed[12] = true;
    }







    // --- Turn/roll helpers ---

    private void startNewTurn(boolean freshGame) {
        hasRolledThisTurn = false;
        rollsLeft = 3;
        Log.d(TAG, "Start new turn (freshGame=" + freshGame + "), rollsLeft=" + rollsLeft);
        tvRollsLeft.setText("Rolls left: " + rollsLeft);
        btnRollAll.setEnabled(true);
        btnRollChosen.setEnabled(true);

        // Default: nothing selected for chosen-rolls
        for (ToggleButton b : dieBtns) b.setChecked(false);
        updateRollChosenEnabled();

        if (freshGame) Arrays.fill(dice, 1);
        syncDiceImages();

        // Clear both left and right column groups
        RadioGroup rgColumnLeft = findViewById(R.id.rgColumnLeft);
        RadioGroup rgColumnRight = findViewById(R.id.rgColumnRight);
        rgColumnLeft.clearCheck();
        rgColumnRight.clearCheck();

        btnCommit.setText("Choose (0 pts)");
        disableUsedCategories(); // keep past choices disabled



    }

    private void disableUsedCategories() {
        RadioButton[] all = {
                rbOnes, rbTwos, rbThrees, rbFours, rbFives, rbSixes,
                rbChance, rbThreeKind, rbFourKind,
                rbFullHouse, rbSmallStraight, rbLargeStraight, rbYahtzee
        };
        for (RadioButton rb : all) {
            if (!rb.isEnabled()) {
                rb.setEnabled(false); // permanently lock used categories
            }
        }
    }

    private void endTurn() {
        boolean anyAvailable = false;
        for (boolean used : categoryUsed) {
            if (!used) {
                anyAvailable = true;
                break;
            }
        }

        Log.d(TAG, "End turn -> anyAvailable=" + anyAvailable + ", score=" + score);

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
            Log.d(TAG, "Starting next turn...");
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
                rbOnes, rbTwos, rbThrees, rbFours, rbFives, rbSixes,
                rbThreeKind, rbFourKind, rbFullHouse,
                rbSmallStraight, rbLargeStraight, rbYahtzee, rbChance
        };

        for (int i = 0; i < all.length; i++) {
            if (categoryUsed[i]) {
                all[i].setEnabled(false); // permanently used
            } else {
                all[i].setEnabled(enable); // only enable if still unused
            }
        }
    }


    private void rollDice(boolean rollAll) {
        if (rollsLeft <= 0) return;
        Log.d(TAG, "rollDice(rollAll=" + rollAll + "), before=" + Arrays.toString(dice));

        boolean[] mask = new boolean[5];
        if (rollAll) {
            Arrays.fill(mask, true);
        } else {
            for (int i = 0; i < 5; i++) mask[i] = dieBtns[i].isChecked();
        }
        rollDiceWithMask(mask);
    }

    // Re-roll using a frozen mask (and never repeat the same face)
    private void rollDiceWithMask(boolean[] mask) {
        Log.d(TAG, "rollDiceWithMask mask=" + Arrays.toString(mask) + " before=" + Arrays.toString(dice));
        for (int i = 0; i < 5; i++) {
            if (mask[i]) {
                int old = dice[i];
                dice[i] = rollDifferentFrom(old); // ensure it changes
                Log.d(TAG, "  -> die[" + i + "] REROLL " + old + " -> " + dice[i]);
            } else {
                Log.d(TAG, "  -> die[" + i + "] HELD at " + dice[i]);
            }
        }

        rollsLeft--;
        hasRolledThisTurn = true;

        // Only enable categories once after the *first* roll in a turn
        if (rollsLeft == 2) {
            enableAllCategories(true);
            disableUsedCategories();
        }


        Log.d(TAG, "After roll: " + Arrays.toString(dice) + "  rollsLeft=" + rollsLeft);

        if (rollsLeft == 0) {
            btnRollAll.setEnabled(false);
            btnRollChosen.setEnabled(false);
            Log.d(TAG, "Roll buttons disabled (no rolls left)");
        }
        tvRollsLeft.setText("Rolls left: " + rollsLeft);
        syncDiceImages();

        int preview = previewSelectedCategoryPoints();
        btnCommit.setText("Choose (" + preview + " pts)");
        updateRollChosenEnabled();
    }

    private void syncDiceImages() {
        for (int i = 0; i < 5; i++) setDieBackgroundForValue(dieBtns[i], dice[i]);
        Log.d(TAG, "syncDiceImages -> " + Arrays.toString(dice));
    }

    private void setDieBackgroundForValue(ToggleButton btn, int value) {
        int resId;
        switch (value) {
            case 1:  resId = R.drawable.die1_selector; break;
            case 2:  resId = R.drawable.die2_selector; break;
            case 3:  resId = R.drawable.die3_selector; break;
            case 4:  resId = R.drawable.die4_selector; break;
            case 5:  resId = R.drawable.die5_selector; break;
            default: resId = R.drawable.die6_selector; break;
        }
        btn.setBackgroundResource(resId);
    }

    // --- Scoring preview/commit ---

    private int previewSelectedCategoryPoints() {
        RadioGroup rgColumnLeft = findViewById(R.id.rgColumnLeft);
        RadioGroup rgColumnRight = findViewById(R.id.rgColumnRight);

        // Check which column has a selected radio button
        int id = rgColumnLeft.getCheckedRadioButtonId();
        if (id == -1) id = rgColumnRight.getCheckedRadioButtonId();
        if (id == -1) return 0;

        // Upper section
        if (id == R.id.rbOnes)   return sumOfFace(1);
        if (id == R.id.rbTwos)   return sumOfFace(2);
        if (id == R.id.rbThrees) return sumOfFace(3);
        if (id == R.id.rbFours)  return sumOfFace(4);
        if (id == R.id.rbFives)  return sumOfFace(5);
        if (id == R.id.rbSixes)  return sumOfFace(6);

        // Lower section
        if (id == R.id.rbYahtzee)        return isYahtzee() ? 50 : 0;
        if (id == R.id.rbChance)         return sumAll();
        if (id == R.id.rbThreeKind)      return hasNOfAKind(3) ? sumAll() : 0;
        if (id == R.id.rbFourKind)       return hasNOfAKind(4) ? sumAll() : 0;
        if (id == R.id.rbFullHouse)      return isFullHouse() ? 25 : 0;
        if (id == R.id.rbSmallStraight)  return isSmallStraight() ? 30 : 0;
        if (id == R.id.rbLargeStraight)  return isLargeStraight() ? 40 : 0;

        return 0;
    }


    private int sumOfFace(int face) {
        int s = 0;
        for (int v : dice) if (v == face) s += face;
        return s;
    }

    private int sumAll() {
        int s = 0;
        for (int v : dice) s += v;
        return s;
    }

    private int[] counts() {
        int[] c = new int[7]; // indices 1..6
        for (int v : dice) c[v]++;
        return c;
    }

    private boolean hasNOfAKind(int n) {
        for (int cnt : counts()) if (cnt >= n) return true;
        return false;
    }

    private boolean isYahtzee() {
        int v0 = dice[0];
        for (int v : dice) if (v != v0) return false;
        return true;
    }

    private boolean isFullHouse() {
        int[] c = counts();
        boolean has3 = false, has2 = false;
        for (int cnt : c) {
            if (cnt == 3) has3 = true;
            if (cnt == 2) has2 = true;
        }
        return has3 && has2;
    }

    private boolean isSmallStraight() {
        // 4-in-a-row: 1-2-3-4 OR 2-3-4-5 OR 3-4-5-6
        boolean[] p = new boolean[7];
        for (int v : dice) p[v] = true;
        return (p[1] && p[2] && p[3] && p[4]) ||
                (p[2] && p[3] && p[4] && p[5]) ||
                (p[3] && p[4] && p[5] && p[6]);
    }

    private boolean isLargeStraight() {
        // 5-in-a-row: 1-2-3-4-5 OR 2-3-4-5-6
        boolean[] p = new boolean[7];
        for (int v : dice) p[v] = true;
        return (p[1] && p[2] && p[3] && p[4] && p[5]) ||
                (p[2] && p[3] && p[4] && p[5] && p[6]);
    }

    // --- Guardrail/helpers ---

    private boolean anyDieSelected() {
        for (ToggleButton b : dieBtns) if (b.isChecked()) return true;
        return false;
    }

    private void updateRollChosenEnabled() {
        btnRollChosen.setEnabled(rollsLeft > 0 && anyDieSelected());
    }

    // Return a random face 1..6 that's different from prev
    private int rollDifferentFrom(int prev) {
        int v = rng.nextInt(6) + 1;
        while (v == prev) v = rng.nextInt(6) + 1;
        return v;
    }
}
