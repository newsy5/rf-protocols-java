package rf.protocols.debug;

import org.bulldog.core.gpio.DigitalInput;
import org.bulldog.core.gpio.base.DigitalIOFeature;
import org.bulldog.core.platform.Board;
import org.bulldog.core.platform.Platform;
import org.bulldog.cubieboard.CubieboardPin;
import org.bulldog.cubieboard.gpio.CubieboardDigitalInput;
import org.bulldog.cubieboard.gpio.CubieboardDigitalOutput;
import rf.protocols.bulldog.BulldogInterruptListener;
import rf.protocols.core.Message;
import rf.protocols.core.MessageListener;
import rf.protocols.core.ProtocolRegistry;
import rf.protocols.core.SignalLevelListener;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

/**
 * @author Eugene Schava <eschava@gmail.com>
 */
public class PrintAllMessages {
    public static void main(String[] args) throws InterruptedException, IOException {
        ProtocolRegistry registry = ProtocolRegistry.getInstance();

        String propertiesFile = System.getProperty("propertiesFile");
        if (propertiesFile != null) {
            Properties props = new Properties();
            props.load(new FileInputStream(propertiesFile));

            for (String key : props.stringPropertyNames())
            {
                String[] parts = key.split(".", 2);
                String protocol = parts[0];
                String name = parts[1];
                String value = props.getProperty(key);

                registry.setProtocolProperty(protocol, name, value);
            }
        }

        Collection<String> protocolNames = registry.getProtocolNames();
        MessageListener<? extends Message> messageListener = new MessageListener<Message>() {
            @Override
            public void onMessage(Message message) {
                System.out.println(message);
            }
        };
        SignalLevelListener signalLevelListener = registry.createListener(messageListener, protocolNames);

        Board board = Platform.createBoard();
        // TODO: pins should be moved to configuration
        CubieboardPin pin = new CubieboardPin("PI14", 68, "I", 14, "68_pi14", true);
        pin.addFeature(new DigitalIOFeature(pin, new CubieboardDigitalInput(pin), new CubieboardDigitalOutput(pin)));
        board.getPins().add(pin);

        DigitalInput input = board.getPin("PI14").as(DigitalInput.class);
        input.setInterruptDebounceMs(-1);
        input.enableInterrupts();
        input.addInterruptListener(new BulldogInterruptListener(signalLevelListener));

        Thread.sleep(1000 * 1000 * 1000);
    }
}