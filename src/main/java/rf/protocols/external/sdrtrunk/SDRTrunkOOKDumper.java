package rf.protocols.external.sdrtrunk;

import controller.ResourceManager;
import controller.channel.Channel;
import controller.channel.ProcessingChain;
import controller.site.Site;
import decode.DecoderType;
import decode.config.DecodeConfigFactory;
import decode.config.DecodeConfiguration;
import rf.protocols.external.ognl.PropertiesConfigurer;
import sample.complex.ComplexBuffer;
import source.SourceType;
import source.config.SourceConfigFactory;
import source.config.SourceConfigTuner;
import source.tuner.Tuner;
import source.tuner.TunerChannelSource;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Eugene Schava <eschava@gmail.com>
 */
public class SDRTrunkOOKDumper {
    public static void main(String[] args) throws IOException {
        SDRTrunkAdapterProperties properties = new SDRTrunkAdapterProperties();
        PropertiesConfigurer propertiesConfigurer = new PropertiesConfigurer(properties);
        // load adapter properties from -Dadapter.PROP parameters
        propertiesConfigurer.loadFromSystemProperties("adapter.");

        final Tuner tuner = createTuner(properties);

        Listener listener = new Listener(properties.threshold);
        tuner.addListener(listener);
        Runtime.getRuntime().addShutdownHook(listener);
    }

    protected static Tuner createTuner(SDRTrunkAdapterProperties properties) {
        ResourceManager resourceManager = new ResourceManager();

        SourceConfigTuner sourceConfiguration = (SourceConfigTuner) SourceConfigFactory.getSourceConfiguration(SourceType.TUNER);
        sourceConfiguration.setFrequency(properties.frequency);

        DecodeConfiguration decodeConfiguration = DecodeConfigFactory.getDecodeConfiguration(DecoderType.NBFM);

        Channel channel = new Channel();
        channel.setEnabled(true);
        channel.setSite(new Site(), false);
        channel.setSourceConfiguration(sourceConfiguration);
        channel.setDecodeConfiguration(decodeConfiguration);
        channel.setResourceManager(resourceManager);

        ProcessingChain chain = channel.getProcessingChain();
        TunerChannelSource source = (TunerChannelSource) chain.getSource();
        return source.getTuner();
    }

    private static class Listener extends Thread implements sample.Listener<ComplexBuffer> {
        private final OOKDecoder decoder;

        private final OutputStream outputStream;
        private final byte[] buffer = new byte[1000000];

        private int index_ = 0;
        private boolean active = false;

        public Listener(float threshold) throws IOException {
            outputStream = new FileOutputStream("dump.raw");
            decoder = new OOKDecoder(threshold);

//            // create streams and write wav header
//            AudioFormat audioFormat = new AudioFormat(44100, 8, 2, true, false);
//            AudioInputStream headerStream = new AudioInputStream(new ByteArrayInputStream(new byte[0]), audioFormat, 1000000);
//            AudioSystem.write(headerStream, AudioFileFormat.Type.WAVE, outputStream);
        }

        @Override
        public void receive(ComplexBuffer complexBuffer) {
            float[] samples = complexBuffer.getSamples();
            for (int i = 0; i < samples.length; i+=2) {
                float sample = samples[i];

                long ind = decoder.process(sample);
                if (ind > 0) {
                    active = true;
                } else if (ind < 0) {
                    active = false;
                }

                buffer[index_++] = (byte) (sample / 2);
                buffer[index_++] = (byte) (active ? 100 : 0);
//                buffer[index_++] = (byte) (ind > 0 ? 50 : (ind < 0 ? -50 : 0));

                if (index_ == buffer.length) {
                    // flush
                    index_ = 0;
                    try {
                        outputStream.write(buffer);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // shutdown
        @Override
        public void run() {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
