package libgdx.thrust.copter;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class ThrustCopter extends ApplicationAdapter
{
    private FPSLogger fpsLogger;

    private SpriteBatch batch;

    private Texture background;

    private OrthographicCamera camera;

    private Sprite backgroundSprite;

    private TextureRegion terrainBelow;

    private TextureRegion terrainAbove;

    private Animation plane;

    private float terrainOffset = 0;

    private float planeAnimTime = 0;

    @Override
    public void create()
    {
        fpsLogger = new FPSLogger();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);

        background = new Texture("background.png");

        backgroundSprite = new Sprite(background);
        backgroundSprite.setPosition(0, 0);

        terrainBelow = new TextureRegion(new Texture("groundGrass.png"));

        terrainAbove = new TextureRegion(terrainBelow);

        // Переворачиваем
        terrainAbove.flip(true, true);

        batch = new SpriteBatch();


        plane = new Animation(0.01f,
                new TextureRegion(new Texture("planeRed1.png")),
                new TextureRegion(new Texture("planeRed2.png")),
                new TextureRegion(new Texture("planeRed3.png")),
                new TextureRegion(new Texture("planeRed2.png")));

        plane.setPlayMode(Animation.PlayMode.LOOP);
    }

    @Override
    public void render()
    {
        Gdx.gl.glClearColor(1, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        fpsLogger.log();

        updateScene();
        drawScene();
    }

    private void drawScene()
    {
        camera.update();

        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        drawBackground();

        drawTerrainBelow();
        drawTerrainAbove();

        batch.draw(plane.getKeyFrame(planeAnimTime), 350, 200);

        batch.end();
    }

    private void drawTerrainAbove()
    {
        batch.draw(terrainAbove, terrainOffset, 480 - terrainAbove.
                getRegionHeight());
        batch.draw(terrainAbove, terrainOffset + terrainAbove.
                getRegionWidth(), 480 - terrainAbove.getRegionHeight());
    }

    private void drawTerrainBelow()
    {
        batch.draw(terrainBelow, terrainOffset, 0);
        batch.draw(terrainBelow, terrainOffset + terrainBelow.
                getRegionWidth(), 0);
    }

    private void updateScene()
    {
        float deltaTime = Gdx.graphics.getDeltaTime();
        planeAnimTime += deltaTime;
        terrainOffset -= 200 * deltaTime;
    }

    private void drawBackground()
    {
        // Выключаем смешивание так как рисуем
        // background-текстуру, которая не накладывается
        // на другие.
        batch.disableBlending();

        batch.draw(background, 0, 0);

        batch.enableBlending();
    }

}
