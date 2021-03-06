package rf.protocols.external.ognl;

import ognl.OgnlException;
import ognl.PropertyAccessor;
import rf.protocols.registry.ProtocolConfigurer;

import java.util.Map;

/**
 * @author Eugene Schava <eschava@gmail.com>
 */
public class PropertiesWithAdapterConfigurer extends PropertiesConfigurer {
    static {
//        MissingObjectPropertyAccessor.install();
        ProtocolRegistryConfigurerPropertyAccessor.install();
        ProtocolPropertyWrapperPropertyAccessor.install();
        AdapterPropertyWrapperPropertyAccessor.install();
    }

    private String adapter;

    public PropertiesWithAdapterConfigurer(Object properties) {
        super(properties);
    }

    @Override
    public void setProperty(String name, String value) {
        if (name.equals("adapter"))
            adapter = value;

        super.setProperty(name, value);
    }

    @Override
    protected Object getProperty(PropertyAccessor accessor, Map context, Object name) throws OgnlException {
        if (name.equals("adapter"))
            return new AdapterPropertyWrapper(adapter);

        if (name.equals("protocol"))
            return ProtocolConfigurer.getInstance();

        return super.getProperty(accessor, context, name);
    }
}
