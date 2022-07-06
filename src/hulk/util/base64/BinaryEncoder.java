package hulk.util.base64;

public interface BinaryEncoder extends Encoder {
	byte[] decode(byte[] var1) throws DecoderException;
}
