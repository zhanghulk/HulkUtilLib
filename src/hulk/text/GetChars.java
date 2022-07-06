package hulk.text;

/**
 * Please implement this interface if your CharSequence has a
 * getChars() method like the one in String that is faster than
 * calling charAt() multiple times.
 */
public interface GetChars
extends CharSequence
{
    /**
     * Exactly like String.getChars(): copy chars <code>start</code>
     * through <code>end - 1</code> from this CharSequence into <code>dest</code>
     * beginning at offset <code>destoff</code>.
     */
    public void getChars(int start, int end, char[] dest, int destoff);
}