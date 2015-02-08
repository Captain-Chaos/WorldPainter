/*     */ package net.minecraft.v1_7_3;
/*     */ 
/*     */ import java.util.Random;
/*     */ 
/*     */ public class NoiseGenerator2
/*     */ {
/*   6 */   private static int[][] d = { { 1, 1, 0 }, { -1, 1, 0 }, { 1, -1, 0 }, { -1, -1, 0 }, { 1, 0, 1 }, { -1, 0, 1 }, { 1, 0, -1 }, { -1, 0, -1 }, { 0, 1, 1 }, { 0, -1, 1 }, { 0, 1, -1 }, { 0, -1, -1 } };
/*     */ 
/*   8 */   private int[] e = new int[512];
/*     */   public double a;
/*     */   public double b;
/*     */   public double c;
/* 231 */   private static final double f = 0.5D * (Math.sqrt(3.0D) - 1.0D);
/* 232 */   private static final double g = (3.0D - Math.sqrt(3.0D)) / 6.0D;
/*     */ 
/*     */   public NoiseGenerator2()
/*     */   {
/*  14 */     this(new Random());
/*     */   }
/*     */ 
/*     */   public NoiseGenerator2(Random paramRandom) {
/*  18 */     this.a = (paramRandom.nextDouble() * 256.0D);
/*  19 */     this.b = (paramRandom.nextDouble() * 256.0D);
/*  20 */     this.c = (paramRandom.nextDouble() * 256.0D);
/*  21 */     for (int i = 0; i < 256; i++) {
/*  22 */       this.e[i] = i;
/*     */     }
/*     */ 
/*  25 */     for (int i = 0; i < 256; i++) {
/*  26 */       int j = paramRandom.nextInt(256 - i) + i;
/*  27 */       int k = this.e[i];
/*  28 */       this.e[i] = this.e[j];
/*  29 */       this.e[j] = k;
/*     */ 
/*  31 */       this.e[(i + 256)] = this.e[i];
/*     */     }
/*     */   }
/*     */ 
/*     */   private static int a(double paramDouble)
/*     */   {
/*  37 */     return paramDouble > 0.0D ? (int)paramDouble : (int)paramDouble - 1;
/*     */   }
/*     */ 
/*     */   private static double a(int[] paramArrayOfInt, double paramDouble1, double paramDouble2) {
/*  41 */     return paramArrayOfInt[0] * paramDouble1 + paramArrayOfInt[1] * paramDouble2;
/*     */   }
/*     */ 
/*     */   public void a(double[] paramArrayOfDouble, double paramDouble1, double paramDouble2, int paramInt1, int paramInt2, double paramDouble3, double paramDouble4, double paramDouble5)
/*     */   {
/* 235 */     int i = 0;
/* 236 */     for (int j = 0; j < paramInt1; j++) {
/* 237 */       double d1 = (paramDouble1 + j) * paramDouble3 + this.a;
/* 238 */       for (int k = 0; k < paramInt2; k++) {
/* 239 */         double d2 = (paramDouble2 + k) * paramDouble4 + this.b;
/*     */ 
/* 242 */         double d3 = (d1 + d2) * f;
/* 243 */         int m = a(d1 + d3);
/* 244 */         int n = a(d2 + d3);
/* 245 */         double d4 = (m + n) * g;
/* 246 */         double d5 = m - d4;
/* 247 */         double d6 = n - d4;
/* 248 */         double d7 = d1 - d5;
/* 249 */         double d8 = d2 - d6;
/*     */         int i1;
/*     */         int i2;
/* 253 */         if (d7 > d8) {
/* 254 */           i1 = 1;
/* 255 */           i2 = 0;
/*     */         }
/*     */         else {
/* 258 */           i1 = 0;
/* 259 */           i2 = 1;
/*     */         }
/*     */ 
/* 264 */         double d9 = d7 - i1 + g;
/* 265 */         double d10 = d8 - i2 + g;
/* 266 */         double d11 = d7 - 1.0D + 2.0D * g;
/* 267 */         double d12 = d8 - 1.0D + 2.0D * g;
/*     */ 
/* 269 */         int i3 = m & 0xFF;
/* 270 */         int i4 = n & 0xFF;
/* 271 */         int i5 = this.e[(i3 + this.e[i4])] % 12;
/* 272 */         int i6 = this.e[(i3 + i1 + this.e[(i4 + i2)])] % 12;
/* 273 */         int i7 = this.e[(i3 + 1 + this.e[(i4 + 1)])] % 12;
/*     */ 
/* 275 */         double d13 = 0.5D - d7 * d7 - d8 * d8;
/*     */         double d14;
/* 276 */         if (d13 < 0.0D) { d14 = 0.0D;
/*     */         } else {
/* 278 */           d13 *= d13;
/* 279 */           d14 = d13 * d13 * a(d[i5], d7, d8);
/*     */         }
/* 281 */         double d15 = 0.5D - d9 * d9 - d10 * d10;
/*     */         double d16;
/* 282 */         if (d15 < 0.0D) { d16 = 0.0D;
/*     */         } else {
/* 284 */           d15 *= d15;
/* 285 */           d16 = d15 * d15 * a(d[i6], d9, d10);
/*     */         }
/* 287 */         double d17 = 0.5D - d11 * d11 - d12 * d12;
/*     */         double d18;
/* 288 */         if (d17 < 0.0D) { d18 = 0.0D;
/*     */         } else {
/* 290 */           d17 *= d17;
/* 291 */           d18 = d17 * d17 * a(d[i7], d11, d12);
/*     */         }
/*     */ 
/* 295 */         paramArrayOfDouble[(i++)] += 70.0D * (d14 + d16 + d18) * paramDouble5;
/*     */       }
/*     */     }
/*     */   }
/*     */ }

/* Location:           /home/pepijn/.m2/repository/org/bukkit/minecraft-server/1.6.6/minecraft-server-1.6.6.jar
 * Qualified Name:     net.minecraft.server.NoiseGenerator2
 * JD-Core Version:    0.6.0
 */