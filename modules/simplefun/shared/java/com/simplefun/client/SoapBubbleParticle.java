package com.simplefun.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * FUN11-7: a soap bubble that drifts up slowly (at most 8 blocks in 160 ticks), wobbles a little and pops with a quiet
 * click when it reaches a ceiling or its end. Pure particle: it blocks nothing and carries nobody.
 */
public class SoapBubbleParticle extends SingleQuadParticle {
  private final double drift;

  SoapBubbleParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite sprite) {
    super(level, x, y, z, sprite);
    this.setSize(0.25f, 0.25f);
    this.quadSize = 0.12f + this.random.nextFloat() * 0.1f;
    this.xd = xd;
    this.yd = Math.min(Math.max(yd, 0.02), 0.06);
    this.zd = zd;
    this.gravity = 0;
    this.friction = 1.0f;
    this.lifetime = 120 + this.random.nextInt(40);
    this.drift = this.random.nextDouble() * Math.PI * 2;
  }

  @Override
  public void tick() {
    this.xo = this.x;
    this.yo = this.y;
    this.zo = this.z;
    if (this.age++ >= this.lifetime) {
      pop();
      return;
    }
    double wobble = Math.sin(this.age * 0.15 + this.drift) * 0.006;
    double wantY = this.yd;
    this.move(this.xd + wobble, wantY, this.zd + wobble);
    this.xd *= 0.96;
    this.zd *= 0.96;
    if (this.y == this.yo && wantY > 0) pop();
  }

  private void pop() {
    this.level.playLocalSound(this.x, this.y, this.z, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.AMBIENT, 0.35f, 1.6f, false);
    this.remove();
  }

  @Override
  protected SingleQuadParticle.Layer getLayer() {
    return SingleQuadParticle.Layer.TRANSLUCENT;
  }

  public static class Provider implements ParticleProvider<SimpleParticleType> {
    private final SpriteSet sprites;

    public Provider(SpriteSet sprites) {
      this.sprites = sprites;
    }

    @Override
    public Particle createParticle(
        SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
      return new SoapBubbleParticle(level, x, y, z, xd, yd, zd, this.sprites.get(random));
    }
  }
}
