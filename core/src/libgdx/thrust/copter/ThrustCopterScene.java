package libgdx.thrust.copter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class ThrustCopterScene extends ScreenAdapter
{
    private final Vector2 DAMPING = new Vector2(0.99f, 0.99f);
    private static final float TAP_DRAW_TIME_MAX = 1.0f;
    private static final int TOUCH_IMPULSE = 500;
    private static final int METEOR_SPEED = 60;

    private TextureAtlas.AtlasRegion background;

    private Texture fuelIndicator;
    private TextureRegion terrainBelow;
    private TextureRegion terrainAbove;
    private TextureRegion tapIndicator;
    private TextureRegion tap1;
    private TextureRegion gameOver;
    private TextureRegion pillarDown;
    private TextureRegion pillarUp;
    private TextureRegion selectedMeteorTexture;

    private Array<Vector2> pillars = new Array<Vector2>();
    Array<TextureAtlas.AtlasRegion> meteorTextures = new Array<TextureAtlas.AtlasRegion>();

    private Animation plane;

    private float terrainOffset;
    private float planeAnimTime;
    private float tapDrawTime;
    private float deltaPosition;
    private float nextMeteorIn;

    private boolean meteorInScene;

    // Скорость
    private Vector2 planeVelocity = new Vector2();
    private Vector2 scrollVelocity = new Vector2();
    private Vector2 planePosition = new Vector2();
    private Vector2 lastPillarPosition = new Vector2();
    private Vector2 planeDefaultPosition = new Vector2();
    private Vector2 gravity = new Vector2();
    private Vector2 meteorPosition = new Vector2();
    private Vector2 meteorVelocity = new Vector2();

    private Vector3 touchPosition = new Vector3();

    private GameState gameState = GameState.INIT;

    private Rectangle planeRect = new Rectangle();
    private Rectangle obstacleRect = new Rectangle();

    private Music music;

    private Sound tapSound;
    private Sound crashSound;
    private Sound spawnSound;

    private SpriteBatch batch;
    private TextureAtlas atlas;
    private OrthographicCamera camera;
    private AssetManager manager;
    private final BitmapFont font;

    private Vector3 pickupTiming = new Vector3();
    private Array<Pickup> pickupsInScene = new Array<Pickup>();
    private Pickup tempPickup;
    private int starCount;
    private float fuelCount;
    private float shieldCount;
    private float score;
    private int fuelPercentage;

    private static enum GameState
    {
        INIT, ACTION, GAME_OVER
    }

    public ThrustCopterScene(ThrustCopter thrustCopter)
    {
        batch = thrustCopter.getBatch();
        atlas = thrustCopter.getAtlas();
        camera = thrustCopter.getCamera();
        manager = thrustCopter.getManager();

        font = manager.get("impact-40.fnt", BitmapFont.class);

        createTextRegions(atlas, manager);

        Sprite backgroundSprite = new Sprite(background);
        backgroundSprite.setPosition(0, 0);

        // Переворачиваем
        terrainAbove.flip(true, true);

        plane = new Animation(0.01f,
                new TextureRegion(atlas.findRegion("planeRed1")),
                new TextureRegion(atlas.findRegion("planeRed2")),
                new TextureRegion(atlas.findRegion("planeRed3")),
                new TextureRegion(atlas.findRegion("planeRed2")));
        plane.setPlayMode(Animation.PlayMode.LOOP);

        music = manager.get("sounds/journey.mp3", Music.class);
        music.setLooping(true);
        music.play();

        tapSound = manager.get("sounds/pop.ogg", Sound.class);
        crashSound = manager.get("sounds/crash.ogg", Sound.class);
        spawnSound = manager.get("sounds/alarm.ogg", Sound.class);

        resetScene();
    }

    @Override
    public void render(float delta)
    {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        updateScene(delta);
        drawScene();
    }

    private void drawScene()
    {
        camera.update();

        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        drawBackground();

        drawPillars();

        drawTerrainBelow();
        drawTerrainAbove();

        batch.draw(plane.getKeyFrame(planeAnimTime), planePosition.x,
                planePosition.y);

        if (tapDrawTime > 0)
        {
            batch.draw(tapIndicator, touchPosition.x - 29.5f,
                    touchPosition.y - 29.5f);
            //29.5 is half width/height of the image
        }

        if (gameState == GameState.INIT)
        {
            batch.draw(tap1, planePosition.x, planePosition.y - 80);
        }

        if (gameState == GameState.GAME_OVER)
        {
            batch.draw(gameOver, 400 - 206, 240 - 80);
        }

        font.draw(batch, ""+((int)shieldCount), 390, 450);

        if (meteorInScene)
        {
            batch.draw(selectedMeteorTexture, meteorPosition.x,
                    meteorPosition.y);
        }

        font.draw(batch, ""+(int)(starCount+score), 700, 450);

        for (Pickup pickup : pickupsInScene)
        {
            batch.draw(pickup.getPickupTexture(), pickup.getPickupPosition().x, pickup.getPickupPosition().y);
        }

        batch.setColor(Color.BLACK);
        batch.draw(fuelIndicator, 10, 350);
        batch.setColor(Color.WHITE);
        batch.draw(fuelIndicator, 10, 350, 0, 0, fuelPercentage, 119);

        batch.end();
    }

    private void drawPillars()
    {
        for (Vector2 vec : pillars)
        {
            if (vec.y == 1)
            {
                batch.draw(pillarUp, vec.x, 0);
            }
            else
            {
                batch.draw(pillarDown, vec.x,
                        480 - pillarDown.getRegionHeight());
            }
        }
    }

    private void resetScene()
    {
        terrainOffset = 0;
        planeAnimTime = 0;

        scrollVelocity.set(4, 0);
        planeVelocity.set(400, 0);
        gravity.set(0, -4);

        starCount = 0;
        score = 0;
        shieldCount = 15;
        fuelCount = 100;
        fuelPercentage = 114;

        planeDefaultPosition.set(300 - 88 / 2, 240 - 73 / 2);
        planePosition.set(planeDefaultPosition.x, planeDefaultPosition.y);

        pillars.clear();
        addPillar();

        meteorInScene = false;
        nextMeteorIn = (float) Math.random() * 5;
    }

    private void updateScene(float deltaTime)
    {
        if (Gdx.input.justTouched())
        {
            tapSound.play();

            if (gameState == GameState.INIT)
            {
                gameState = GameState.ACTION;
                return;
            }

            if (gameState == GameState.GAME_OVER)
            {

                gameState = GameState.INIT;
                resetScene();
                return;
            }

            if (fuelCount > 0)
            {
                Vector2 tmpVector = new Vector2();

                touchPosition.set(Gdx.input.getX(), Gdx.input.getY(), 0);
                camera.unproject(touchPosition);

                tmpVector.set(planePosition.x, planePosition.y);
                tmpVector.sub(touchPosition.x, touchPosition.y).nor();

                planeVelocity.mulAdd(tmpVector,
                        TOUCH_IMPULSE - MathUtils.clamp(Vector2.dst(touchPosition.x,
                                        touchPosition.y, planePosition.x, planePosition.y), 0,
                                TOUCH_IMPULSE));

                tapDrawTime = TAP_DRAW_TIME_MAX;
            }
        }

        if (gameState == GameState.INIT || gameState ==
                GameState.GAME_OVER)
        {
            return;
        }

        planeAnimTime += deltaTime;

        planeVelocity.scl(DAMPING);
        planeVelocity.add(gravity);
        planeVelocity.add(scrollVelocity);

        planePosition.mulAdd(planeVelocity, deltaTime);

        deltaPosition = planePosition.x - planeDefaultPosition.x;
        terrainOffset -= deltaPosition;

        planePosition.x = planeDefaultPosition.x;

        if (terrainOffset * -1 > terrainBelow.getRegionWidth())
        {
            terrainOffset = 0;
        }

        if (terrainOffset > 0)
        {
            terrainOffset = -terrainBelow.getRegionWidth();
        }

        if (planePosition.y < terrainBelow.getRegionHeight() - 35 || planePosition.y + 73 > 480 -
                terrainBelow.getRegionHeight() + 35)
        {
            if (gameState != GameState.GAME_OVER)
            {
                crashSound.play();
                gameState = GameState.GAME_OVER;
            }
        }

        pillarsLogic();

        for (Pickup pickup : pickupsInScene)
        {
            pickup.getPickupPosition().x -= deltaPosition;

            if (pickup.getPickupPosition().x + pickup.getPickupTexture().getRegionWidth() < -10)
            {
                pickupsInScene.removeValue(pickup, false);
            }

            obstacleRect.set(pickup.getPickupPosition().x,
                    pickup.getPickupPosition().y,
                    pickup.getPickupTexture().getRegionWidth(),
                    pickup.getPickupTexture().getRegionHeight());

            if (planeRect.overlaps(obstacleRect))
            {
                pickIt(pickup);
            }
        }

        meteorLogic(deltaTime);

        tapDrawTime -= deltaTime;

        checkAndCreatePickup(deltaTime);
        fuelCount -= 6 * deltaTime;
        fuelPercentage = (int) (114 * fuelCount / 100);
        shieldCount -= deltaTime;
    }

    private void meteorLogic(float deltaTime)
    {
        if (meteorInScene)
        {
            meteorPosition.mulAdd(meteorVelocity, deltaTime);

            meteorPosition.x -= deltaPosition;

            if (meteorPosition.x < -10)
            {
                meteorInScene = false;
            }
        }

        nextMeteorIn -= deltaTime;

        if (nextMeteorIn <= 0)
        {
            launchMeteor();
        }

        if (meteorInScene)
        {
            obstacleRect.set(meteorPosition.x + 2, meteorPosition.y + 2,
                    selectedMeteorTexture.getRegionWidth() - 4,
                    selectedMeteorTexture.getRegionHeight() - 4);

            if (planeRect.overlaps(obstacleRect))
            {
                if (gameState != GameState.GAME_OVER)
                {
                    crashSound.play();
                    gameState = GameState.GAME_OVER;
                }
            }
        }
    }

    private void launchMeteor()
    {
        nextMeteorIn = 1.5f + (float) Math.random() * 5;

        if (meteorInScene)
        {
            return;
        }

        spawnSound.play();

        meteorInScene = true;

        int id = (int) (Math.random() * meteorTextures.size);
        selectedMeteorTexture = meteorTextures.get(id);

        meteorPosition.x = 810;
        meteorPosition.y = (float) (80 + Math.random() * 320);

        Vector2 destination = new Vector2();
        destination.x = -10;
        destination.y = (float) (80 + Math.random() * 320);
        destination.sub(meteorPosition).nor();

        meteorVelocity.mulAdd(destination, METEOR_SPEED);
    }

    private void pillarsLogic()
    {
        planeRect.set(planePosition.x + 16, planePosition.y, 50, 73);

        for (Vector2 vec : pillars)
        {
            vec.x -= deltaPosition;

            if (vec.x + pillarUp.getRegionWidth() < -10)
            {
                pillars.removeValue(vec, false);
            }

            if (vec.y == 1)
            {
                obstacleRect.set(vec.x + 10, 0, pillarUp.getRegionWidth() - 20,
                        pillarUp.getRegionHeight() - 10);
            }
            else
            {
                obstacleRect.set(vec.x + 10,
                        480 - pillarDown.getRegionHeight() + 10,
                        pillarUp.getRegionWidth() - 20, pillarUp.getRegionHeight());
            }

            if (planeRect.overlaps(obstacleRect))
            {
                if (gameState != GameState.GAME_OVER)
                {
                    crashSound.play();
                    gameState = GameState.GAME_OVER;
                }
            }
        }

        if (lastPillarPosition.x < 400)
        {
            addPillar();
        }
    }

    private void addPillar()
    {
        Vector2 pillarPosition = new Vector2();

        if (pillars.size == 0)
        {
            pillarPosition.x = (float) (800 + Math.random() * 600);
        }
        else
        {
            pillarPosition.x = lastPillarPosition.x + (float) (600 +
                    Math.random() * 600);
        }

        if (MathUtils.randomBoolean())
        {
            pillarPosition.y = 1;
        }
        else
        {
            pillarPosition.y = -1; //upside down
        }

        lastPillarPosition = pillarPosition;

        pillars.add(pillarPosition);
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

    private void createTextRegions(TextureAtlas atlas, AssetManager manager)
    {
        fuelIndicator = manager.get("life.png", Texture.class);
        gameOver = new TextureRegion(manager.get("gameover.png", Texture.class));

        background = atlas.findRegion("background");
        tapIndicator = atlas.findRegion("tap2");
        tap1 = atlas.findRegion("tap1");

        terrainBelow = atlas.findRegion("groundGrass");
        terrainAbove = new TextureRegion(terrainBelow);

        pillarUp = atlas.findRegion("rockGrassUp");
        pillarDown = atlas.findRegion("rockGrassDown");

        initMeteorTextures(atlas);
    }

    private void initMeteorTextures(TextureAtlas atlas)
    {
        meteorTextures.add(atlas.findRegion("meteorBrown_med1"));
        meteorTextures.add(atlas.findRegion("meteorBrown_med2"));
        meteorTextures.add(atlas.findRegion("meteorBrown_small1"));
        meteorTextures.add(atlas.findRegion("meteorBrown_small2"));
        meteorTextures.add(atlas.findRegion("meteorBrown_tiny1"));
        meteorTextures.add(atlas.findRegion("meteorBrown_tiny2"));
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

    private void checkAndCreatePickup(float delta)
    {
        pickupTiming.sub(delta);

        if (pickupTiming.x <= 0)
        {
            pickupTiming.x = (float) (0.5 + Math.random() * 0.5);
            if (addPickup(Pickup.STAR))
                pickupTiming.x = 1 + (float) Math.random() * 2;
        }
        if (pickupTiming.y <= 0)
        {
            pickupTiming.y = (float) (0.5 + Math.random() * 0.5);
            if (addPickup(Pickup.FUEL))
                pickupTiming.y = 3 + (float) Math.random() * 2;
        }
        if (pickupTiming.z <= 0)
        {
            pickupTiming.z = (float) (0.5 + Math.random() * 0.5);
            if (addPickup(Pickup.SHIELD))
                pickupTiming.z = 10 + (float) Math.random() * 3;
        }
    }

    private boolean addPickup(int pickupType)
    {
        Vector2 randomPosition = new Vector2();
        randomPosition.x = 820;
        randomPosition.y = (float) (80 + Math.random() * 320);

        for (Vector2 vec : pillars)
        {
            if (vec.y == 1)
            {
                obstacleRect.set(vec.x, 0, pillarUp.getRegionWidth(), pillarUp.getRegionHeight());
            }
            else
            {
                obstacleRect.set(vec.x, 480 - pillarDown.getRegionHeight(), pillarUp.getRegionWidth(), pillarUp.getRegionHeight());
            }

            if (obstacleRect.contains(randomPosition))
            {
                return false;
            }
        }

        tempPickup = new Pickup(pickupType, manager);
        tempPickup.getPickupPosition().set(randomPosition);
        pickupsInScene.add(tempPickup);

        return true;
    }

    private void pickIt(Pickup pickup)
    {
        pickup.getPickupSound().play();

        switch (pickup.getPickupType())
        {
            case Pickup.STAR:
                starCount += pickup.getPickupValue();
                break;

            case Pickup.SHIELD:
                shieldCount = pickup.getPickupValue();
                break;

            case Pickup.FUEL:
                fuelCount = pickup.getPickupValue();
                break;
        }

        pickupsInScene.removeValue(pickup, false);
    }

    @Override
    public void dispose()
    {
        tapSound.dispose();
        crashSound.dispose();
        spawnSound.dispose();
        music.dispose();
        pillars.clear();
        meteorTextures.clear();
    }

}
