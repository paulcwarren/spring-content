package internal.org.springframework.content.commons.renditions;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.util.MimeType;

public class RenditionServiceImpl implements RenditionService {

    private List<RenditionProvider> providers = new ArrayList<>();

    @Autowired(required=false)
    public RenditionServiceImpl(RenditionProvider... providers) {
        for (RenditionProvider provider : providers) {
            this.providers.add(provider);
        }
    }

    @Override
    public boolean canConvert(String fromMimeType, String toMimeType) {
        for (RenditionProvider provider : providers) {
            if (MimeType.valueOf(fromMimeType).includes(MimeType.valueOf(provider.consumes()))) {
                for (String produce : provider.produces()) {
                    if (MimeType.valueOf(toMimeType).includes(MimeType.valueOf(produce))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String[] conversions(String fromMimeType) {
        Set<String> conversions = new HashSet<>();
        for (RenditionProvider provider : providers) {
            if (provider.consumes().equals(fromMimeType)) {
                conversions.addAll(Arrays.asList(provider.produces()));
            }
        }
        return conversions.toArray(new String[] {});
    }

    @Override
    public InputStream convert(String fromMimeType, InputStream fromInputSource, String toMimeType) {
        for (RenditionProvider provider : providers) {
            if (MimeType.valueOf(fromMimeType)
                    .includes(MimeType.valueOf(provider.consumes()))) {
                for (String produce : provider.produces()) {
                    if (MimeType.valueOf(toMimeType)
                            .includes(MimeType.valueOf(produce))) {
                        return provider.convert(fromInputSource, toMimeType);
                    }
                }
            }
        }
        return null;
    }

}
