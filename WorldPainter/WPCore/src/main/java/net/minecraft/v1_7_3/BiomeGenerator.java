/*     */ package net.minecraft.v1_7_3;
/*     */ 
/*     */ import java.util.Random;
/*     */ 
/*     */ public class BiomeGenerator
/*     */ {
/*     */   private NoiseGeneratorOctaves2 e;
/*     */   private NoiseGeneratorOctaves2 f;
/*     */   private NoiseGeneratorOctaves2 g;
/*     */   public double[] a;
/*     */   public double[] b;
/*     */   public double[] c;
/*     */   public int[] biomesArray;
/*     */ 
/*     */   protected BiomeGenerator()
/*     */   {
/*     */   }
/*     */ 
/*     */   public BiomeGenerator(long seed)
/*     */   {
/*  17 */     this.e = new NoiseGeneratorOctaves2(new Random(seed * 9871L), 4);
/*  18 */     this.f = new NoiseGeneratorOctaves2(new Random(seed * 39811L), 4);
/*  19 */     this.g = new NoiseGeneratorOctaves2(new Random(seed * 543321L), 2);
/*     */   }
/*     */ 
/*     */   public int getBiome(int x, int z) {
/*  39 */     return getBiomes(x, z, 1, 1)[0];
/*     */   }
/*     */ 
/*     */   public int[] getBiomes(int x, int z, int width, int length)
/*     */   {
/*  48 */     this.biomesArray = getBiomes(this.biomesArray, x, z, width, length);
/*  49 */     return this.biomesArray;
/*     */   }
/*     */ 
/*     */   public int[] getBiomes(int[] biomesArray, int x, int z, int width, int length)
/*     */   {
/*  93 */     if ((biomesArray == null) || (biomesArray.length < width * length)) {
/*  94 */       biomesArray = new int[width * length];
/*     */     }
/*     */ 
/*  97 */     this.a = this.e.a(this.a, x, z, width, width, 0.025000000372529D, 0.025000000372529D, 0.25D);
/*  98 */     this.b = this.f.a(this.b, x, z, width, width, 0.0500000007450581D, 0.0500000007450581D, 0.3333333333333333D);
/*  99 */     this.c = this.g.a(this.c, x, z, width, width, 0.25D, 0.25D, 0.5882352941176471D);
/*     */ 
/* 101 */     int i = 0;
/* 102 */     for (int j = 0; j < width; j++) {
/* 103 */       for (int k = 0; k < length; k++)
/*     */       {
/* 108 */         double d1 = this.c[i] * 1.1D + 0.5D;
/*     */ 
/* 110 */         double d2 = 0.01D;
/* 111 */         double d3 = 1.0D - d2;
/* 112 */         double d4 = (this.a[i] * 0.15D + 0.7D) * d3 + d1 * d2;
/* 113 */         d2 = 0.002D;
/* 114 */         d3 = 1.0D - d2;
/* 115 */         double d5 = (this.b[i] * 0.15D + 0.5D) * d3 + d1 * d2;
/* 116 */         d4 = 1.0D - (1.0D - d4) * (1.0D - d4);
/*     */ 
/* 119 */         if (d4 < 0.0D) d4 = 0.0D;
/* 120 */         if (d5 < 0.0D) d5 = 0.0D;
/* 121 */         if (d4 > 1.0D) d4 = 1.0D;
/* 122 */         if (d5 > 1.0D) d5 = 1.0D;
/*     */ 
/* 124 */         this.a[i] = d4;
/* 125 */         this.b[i] = d5;
/*     */ 
/* 127 */         biomesArray[(i++)] = BiomeBase.getBiome(d4, d5);
/*     */       }
/*     */ 
/*     */     }
/*     */ 
/* 133 */     return biomesArray;
/*     */   }
/*     */ }

/* Location:           /home/pepijn/.m2/repository/org/bukkit/minecraft-server/1.6.6/minecraft-server-1.6.6.jar
 * Qualified Name:     net.minecraft.server.BiomeGenerator
 * JD-Core Version:    0.6.0
 */