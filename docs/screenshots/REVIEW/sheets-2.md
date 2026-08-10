Excellent. Let's get straight to it. As a senior product designer, my job is to be clear and direct. This product deals with financial data for audits, so clarity and trust are non-negotiable. The owner's complaint about the app looking "sad" and "not correct" is accurate. The foundation is there, but the execution of the visual language is failing.

Here is my critique of the provided batch.

### 1. TYPE

The typographic system is the single biggest problem in this entire app. It is the primary reason the design feels "sad," unprofessional, and hard to use.

*   **Hierarchy is illegible.** Because a monospace font is used for everything, there is no typographic texture to distinguish a title from body copy, or data from a label. The only differentiators are size and colour, which isn't enough. On `permission_primer_sheet`, the headline "Mileway needs your location to work" and the paragraph below it are typographically identical, creating a flat, monotonous block that is tiring to read.
*   **Monospace hurts readability.** Monospace fonts are designed for code and tabular data, not for prose. The wide, uneven spacing between letters makes paragraphs (`permission_primer_sheet`, `whats_new_sheet`) a chore to read. It makes titles (`Choose Vehicle Type`, `List of Centers`) look like command-line prompts, not professional headings. It strips the product of any personality beyond "unfinished tech demo."
*   **Monospace helps (in one specific place).** The *only* place this font choice works is for the numerical data in `smart_distance_sheet` (`12.0 km`, `58%`, `19.0 km`). Here, the uniform width of the characters ensures that numbers align neatly and don't jump around as they update. This is the correct use case. However, because everything else is also monospace, this benefit is completely lost.

**Recommendation:** Immediately replace the monospace font with a proper variable-width sans-serif UI font (e.g., Inter, Roboto, SF Pro) for ALL UI elements: titles, body copy, buttons, labels. Retain the monospace font *only* for displaying numerical outputs where alignment is critical, like the data points in `smart_distance_sheet`. This one change will have the most dramatic impact on the app's professionalism and usability.

### 2. COLOUR

The "Ember" theme is conceptually fine, but its application is weak and contributes to the "sad" feeling.

*   **The amber accent is exhausting.** It's used for primary buttons, icons, selection states, and even informational icons (`whats_new_sheet`). When one colour means "action," "information," "warning," and "selected," it means nothing. It's visual noise.
*   **The tonal range is non-existent.** The UI is composed of near-black, a slightly different near-black, and amber. This lack of depth makes the interface feel flat, lifeless, and claustrophobic. There are no shades of grey to create visual separation between surfaces (background, sheet, cards).
*   **Contrast fails.** The most egregious example is in the pickers. On `wheel_date_picker_dialog`, the unselected dates/years are so low-contrast they are functionally invisible. This is an accessibility failure. On `session_restore_sheet`, the secondary metadata ("tok-8f2", "2 hours ago") is also too low-contrast, making it difficult to read. The red text on the dark red card in `policy_violation_sheet` is also borderline.

**Recommendation:**
1.  **Introduce more dark grey tones.** Use a very dark grey for the main background, a slightly lighter grey for the sheet surface, and another step lighter for cards within the sheet. This will create depth and hierarchy.
2.  **Be disciplined with the amber accent.** It should mean one thing: **the primary, progressive action on the screen**. That's it. All other interactive elements should use neutral or less-emphasized styles.

### 3. BUTTONS

The button strategy is inconsistent and, in one critical case, dangerous.

*   **No clear primary on some screens.** `session_restore_sheet` presents two bright amber "Restore" buttons, competing for attention and creating cognitive overload. Which draft is more important? The UI offers no guidance.
*   **Wrong emphasis.** On `policy_violation_sheet`, the radio buttons are presented inside massive, outlined containers. This is a very heavy-handed treatment for a simple selection. Standard radio buttons would be far cleaner and less visually jarring.
*   **Contradictory styling.** `submit_confirm_sheet` is a design system disaster. The primary action ("Submit Miles") and the secondary action ("Cancel") are styled almost identically: dark, filled rectangles with an icon. This is a classic dark pattern, even if unintentional. It dramatically increases the chance of a user tapping the wrong button on a crucial confirmation step. This single screen breaks the button language established elsewhere (e.g., `stranger_session_sheet`'s clear "Resume Session" vs "Not Now").

**Recommendation:**
1.  Immediately fix `submit_confirm_sheet`. The primary action should be the solid amber button. "Cancel" should be a text-only button or a subtly outlined one.
2.  Rethink the `session_restore_sheet` layout. Perhaps only one session can be restored at a time, or the primary "Restore" should be de-emphasized until a card is explicitly tapped.
3.  Use standard, lightweight components for selection controls like on `policy_violation_sheet`.

### 4. DENSITY AND RHYTHM

The spacing and alignment are inconsistent, making the app feel haphazard.

*   **Too cramped.** `sos_bottom_sheet` is a key offender. The elements are jammed together with no breathing room, which undermines the seriousness of an "Emergency SOS" feature.
*   **Inconsistent alignment.** The app randomly switches between center-alignment (`permission_primer_sheet`, `stranger_session_sheet`) and left-alignment (`policy_violation_sheet`, `vendor_picker_sheet`). Center-aligned text is difficult to read for anything longer than two lines.
*   **No vertical rhythm.** There is no consistent spacing system for margins and padding between elements. Look at the space above the title on `policy_violation_sheet` versus `vehicle_picker_sheet`. It feels arbitrary.

**Recommendation:**
1.  Establish a spacing scale (e.g., 4dp/8dp/16dp/24dp) and apply it consistently for all padding and margins.
2.  Default to left-alignment for all content that isn't a single, short title or a set of buttons. It improves scannability tenfold.

### 5. "SAD vs ALIVE"

The owner is right. The app feels lifeless. Here's exactly why, and how to fix it without making it "toy-like."

*   **It's "sad" because it's flat.** There is no elevation. The sheets, cards, and buttons are just coloured rectangles on top of each other. Nothing feels tactile or physically distinct.
    *   **Fix:** Add subtle drop shadows to the bottom sheets to lift them off the background scrim. Add a very subtle shadow to the primary amber buttons to make them look pressable. This creates depth and a sense of physical reality.
*   **It's "sad" because it's monotonous.** The single font and overuse of amber create a boring, repetitive visual texture.
    *   **Fix:** The typography change is #1. #2 is accent colour discipline. This introduces variety and guides the user's eye.
*   **It's "sad" because it lacks humanity.** It's a tool, but it doesn't have to be sterile.
    *   **Fix:** On screens like `permission_primer_sheet`, add a simple, on-brand spot illustration. A clean line-art icon showing a map route would do wonders to explain the *why* behind the permission request and add a touch of personality.

### 6. THE MOMENT THAT MATTERS

Of the sheets provided, the highest-stakes interactions are `policy_violation_sheet` and `submit_confirm_sheet`.

*   `policy_violation_sheet` is a moment of friction. The user did something wrong. The UI's job is to explain the problem clearly and provide a path to resolution. The current design is functional but clumsy. The red card works, but the typography and oversized radio buttons weaken its clarity.
*   `submit_confirm_sheet` is the final gate before a permanent action. This is a moment that demands absolute, unambiguous clarity. **The current design fails this test completely.** The button styling is confusing and inconsistent with the rest of the app, making user error highly likely. It's the most critical failure in this batch.

---

### HIGHEST-IMPACT CHANGES (RANKED) & BROKEN SCREENS

1.  **OVERHAUL TYPOGRAPHY.** Replace the global monospace font with a proper sans-serif UI font. This is the highest-leverage change you can make. It will instantly improve legibility, hierarchy, and professionalism on **every single screen**.
2.  **FIX THE BUTTON SYSTEM.** The inconsistent and confusing buttons, especially on `submit_confirm_sheet`, are a critical UX flaw. Standardize on: solid amber for primary, outlined for secondary, and text-only for tertiary actions. This fixes `submit_confirm_sheet`, `session_restore_sheet`, and `policy_violation_sheet`.
3.  **INTRODUCE ELEVATION AND TONAL RANGE.** Use subtle shadows and a broader palette of dark greys to create depth. This will address the "sad" and "flat" feeling across **all sheets**.
4.  **ENFORCE ACCENT COLOR DISCIPLINE.** Use amber for primary CTAs only. This will improve clarity and reduce visual noise on screens like `whats_new_sheet` and `policy_violation_sheet`.

### BROKEN SCREENS

*   **`submit_confirm_sheet`:** BROKEN. The button design is functionally unacceptable for a confirmation dialog. It directly contradicts the established visual language and invites user error on a critical action.
*   **`wheel_date_picker_dialog` & `wheel_time_picker_dialog`:** BROKEN. The extremely low contrast of unselected items is a severe accessibility failure, rendering the component unusable for many users.