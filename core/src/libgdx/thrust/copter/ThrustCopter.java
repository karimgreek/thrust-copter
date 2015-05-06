package libgdx.thrust.copter;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * @author Skurishin Vladislav
 * @since 06.05.15
 */
public class ThrustCopter extends Game
{
    private static final int screenWidth = 800;
    private static final int screenHeight = 480;

    private FPSLogger fpsLogger;

    private Viewport viewport;

    private OrthographicCamera camera;

    private SpriteBatch batch;

    private TextureAtlas atlas;

    private AssetManager manager = new AssetManager();

    public ThrustCopter()
    {
        fpsLogger = new FPSLogger();
        createCamera();
    }

    @Override
    public void create()
    {
        initAssetManager();

        batch = new SpriteBatch();
        atlas = manager.get("ThrustCopter.pack", TextureAtlas.class);

        setScreen(new ThrustCopterScene(this));
    }

    private void initAssetManager()
    {
        manager.load("gameover.png", Texture.class);
        manager.load("sounds/journey.mp3", Music.class);
        manager.load("sounds/pop.ogg", Sound.class);
        manager.load("sounds/crash.ogg", Sound.class);
        manager.load("sounds/alarm.ogg", Sound.class);
        manager.load("ThrustCopter.pack", TextureAtlas.class);

        manager.finishLoading();
    }

    @Override
    public void render()
    {
        fpsLogger.log();
        super.render();
    }

    @Override
    public void resize(int width, int height)
    {
        viewport.update(width, height);
    }

    @Override
    public void dispose()
    {
        batch.dispose();
        atlas.dispose();
    }

    private void createCamera()
    {
        camera = new OrthographicCamera();
        camera.position.set(screenWidth / 2, screenHeight / 2, 0);
        viewport = new FitViewport(screenWidth, screenHeight, camera);
    }

    public static int getScreenWidth()
    {
        return screenWidth;
    }

    public static int getScreenHeight()
    {
        return screenHeight;
    }

    public OrthographicCamera getCamera()
    {
        return camera;
    }

    public SpriteBatch getBatch()
    {
        return batch;
    }

    public TextureAtlas getAtlas()
    {
        return atlas;
    }

    public AssetManager getManager()
    {
        return manager;
    }
}
