package com.trivocab.ielts.domain;

/**
 * Learning workflow selected by the user.
 *
 * <p>{@link #SIMPLE} keeps the original mixed queue (due words first, then new
 * words, four-rating feedback). {@link #IMMERSIVE} follows the "不背单词"-style
 * intensive flow: multiple-choice first encounter with look-alike distractors,
 * repeated in-group recall, and a spelling round before a word is mastered.</p>
 */
public enum LearningMode {
    SIMPLE,
    IMMERSIVE
}
