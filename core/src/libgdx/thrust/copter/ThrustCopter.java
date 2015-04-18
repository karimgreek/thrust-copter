package libgdx.thrust.copter;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ThrustCopter extends ApplicationAdapter
{
    private FPSLogger fpsLogger;

    private SpriteBatch batch;

    private TextureAtlas.AtlasRegion background;

    private OrthographicCamera camera;

    private TextureRegion terrainBelow;
    private TextureRegion terrainAbove;

    private Animation plane;

    private float terrainOffset;
    private float planeAnimTime;

    // Скорость
    private Vector2 planeVelocity = new Vector2();
    private Vector2 planePosition = new Vector2();
    private Vector2 planeDefaultPosition = new Vector2();
    private Vector2 gravity = new Vector2();

    private Viewport viewport;

    private final Vector2 damping = new Vector2(0.99f, 0.99f);

    @Override
    public void create()
    {
        fpsLogger = new FPSLogger();

        camera = new OrthographicCamera();
        camera.position.set(400,240,0);
        viewport = new FitViewport(800, 480, camera);

        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("ThrustCopter.pack"));

        background = atlas.findRegion("background");

        Sprite backgroundSprite = new Sprite(background);
        backgroundSprite.setPosition(0, 0);

        terrainBelow = new TextureRegion(atlas.findRegion("groundGrass"));

        terrainAbove = new TextureRegion(terrainBelow);

        // Переворачиваем
        terrainAbove.flip(true, true);

        batch = new SpriteBatch();


        plane = new Animation(0.01f,
                new TextureRegion(atlas.findRegion("planeRed1")),
                new TextureRegion(atlas.findRegion("planeRed2")),
                new TextureRegion(atlas.findRegion("planeRed3")),
                new TextureRegion(atlas.findRegion("planeRed2")));

        plane.setPlayMode(Animation.PlayMode.LOOP);

        resetScene();
    }

    @Override
    public void resize (int width, int height)
    {
        viewport.update(width, height);
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

        batch.draw(plane.getKeyFrame(planeAnimTime), planePosition.x,
                planePosition.y);

        batch.end();
    }

    private void resetScene()
    {
        terrainOffset = 0;
        planeAnimTime = 0;

        planeVelocity.set(400, 0);
        gravity.set(0, -4);

        planeDefaultPosition.set(400 - 88 / 2, 240 - 73 / 2);
        planePosition.set(planeDefaultPosition.x, planeDefaultPosition.y);
    }

    private void updateScene()
    {
        float deltaTime = Gdx.graphics.getDeltaTime();

        planeAnimTime += deltaTime;

        planeVelocity.scl(damping);
        planeVelocity.add(gravity);

        planePosition.mulAdd(planeVelocity, deltaTime);

        terrainOffset -= planePosition.x - planeDefaultPosition.x;

        planePosition.x = planeDefaultPosition.x;

        if (terrainOffset * -1 > terrainBelow.getRegionWidth())
        {
            terrainOffset = 0;
        }

        if (terrainOffset > 0)
        {
            terrainOffset = -terrainBelow.getRegionWidth();
        }
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
