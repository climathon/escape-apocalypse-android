package de.hackerstolz.climathon;

import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;

public class HorizontalArFragment extends ArFragment {
    @Override
    protected Config getSessionConfiguration(Session session) {
        Config config = new Config(session);
        config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL);
        return config;
    }
}
