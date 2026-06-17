package hugo.brua.homepage;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Homepage implements ModInitializer {
	public static final String MOD_ID = "homepage";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[homepage] Hello Fabric world!");
	}
}
