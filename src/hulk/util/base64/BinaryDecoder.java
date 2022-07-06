package hulk.util.base64;

public interface BinaryDecoder extends Decoder {
	byte[] encode(byte[] var1) throws EncoderException;
}
