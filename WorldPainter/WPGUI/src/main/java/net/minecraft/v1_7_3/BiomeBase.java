/*     */ package net.minecraft.v1_7_3;
/*     */ 
/*     */ import java.util.ArrayList;
/*     */ import java.util.List;
/*     */ 
/*     */ public class BiomeBase
/*     */ {
/*  26 */   public static final int RAINFOREST = 13;
/*  27 */   public static final int SWAMPLAND = 1;
/*  28 */   public static final int SEASONAL_FOREST = 2;
/*  29 */   public static final int FOREST = 3;
/*  30 */   public static final int SAVANNA = 4;
/*  31 */   public static final int SHRUBLAND = 5;
/*  32 */   public static final int TAIGA = 6;
/*  33 */   public static final int DESERT = 7;
/*  34 */   public static final int PLAINS = 8;
/*  35 */   public static final int ICE_DESERT = 9;
/*  36 */   public static final int TUNDRA = 10;
/*     */ 
/*  38 */   public static final int HELL = 11;
/*  39 */   public static final int SKY = 12;
/*     */   public String n;
/*     */   public int o;
/*  45 */   public int r = 5169201;
/*     */ 
/*  47 */   protected List s = new ArrayList();
/*  48 */   protected List t = new ArrayList();
/*  49 */   protected List u = new ArrayList();
/*     */   private boolean v;
/*  51 */   private boolean w = true;
/*     */ 
/*  53 */   private static int[] x = new int[4096];
/*     */ 
/*     */   private BiomeBase e() {
/*  74 */     this.w = false;
/*  75 */     return this;
/*     */   }
/*     */ 
/*     */   public static void a() {
/*  79 */     for (int i = 0; i < 64; i++) {
/*  80 */       for (int j = 0; j < 64; j++) {
/*  81 */         x[(i + j * 64)] = a(i / 63.0F, j / 63.0F);
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */   protected BiomeBase b() {
/* 101 */     this.v = true;
/* 102 */     return this;
/*     */   }
/*     */ 
/*     */   protected BiomeBase a(String paramString) {
/* 106 */     this.n = paramString;
/* 107 */     return this;
/*     */   }
/*     */ 
/*     */   protected BiomeBase a(int paramInt) {
/* 111 */     this.r = paramInt;
/* 112 */     return this;
/*     */   }
/*     */ 
/*     */   protected BiomeBase b(int paramInt) {
/* 116 */     this.o = paramInt;
/* 117 */     return this;
/*     */   }
/*     */ 
/*     */   public static int getBiome(double paramDouble1, double paramDouble2) {
/* 121 */     int i = (int)(paramDouble1 * 63.0D);
/* 122 */     int j = (int)(paramDouble2 * 63.0D);
/* 123 */     return x[(i + j * 64)];
/*     */   }
/*     */ 
/*     */   public static int a(float paramFloat1, float paramFloat2) {
/* 127 */     paramFloat2 *= paramFloat1;
/* 128 */     if (paramFloat1 < 0.1F)
/* 129 */       return TUNDRA;
/* 130 */     if (paramFloat2 < 0.2F) {
/* 131 */       if (paramFloat1 < 0.5F)
/* 132 */         return TUNDRA;
/* 133 */       if (paramFloat1 < 0.95F) {
/* 134 */         return SAVANNA;
/*     */       }
/* 136 */       return DESERT;
/*     */     }
/* 138 */     if ((paramFloat2 > 0.5F) && (paramFloat1 < 0.7F))
/* 139 */       return SWAMPLAND;
/* 140 */     if (paramFloat1 < 0.5F)
/* 141 */       return TAIGA;
/* 142 */     if (paramFloat1 < 0.97F) {
/* 143 */       if (paramFloat2 < 0.35F) {
/* 144 */         return SHRUBLAND;
/*     */       }
/* 146 */       return FOREST;
/*     */     }
/*     */ 
/* 149 */     if (paramFloat2 < 0.45F)
/* 150 */       return PLAINS;
/* 151 */     if (paramFloat2 < 0.9F) {
/* 152 */       return SEASONAL_FOREST;
/*     */     }
/* 154 */     return RAINFOREST;
/*     */   }
/*     */ 
/*     */   public boolean c()
/*     */   {
/* 195 */     return this.v;
/*     */   }
/*     */ 
/*     */   public boolean d() {
/* 199 */     if (this.v) return false;
/* 200 */     return this.w;
/*     */   }
/*     */ 
/*     */   static
/*     */   {
/*  90 */     a();
/*     */   }
/*     */ }

/* Location:           /home/pepijn/.m2/repository/org/bukkit/minecraft-server/1.6.6/minecraft-server-1.6.6.jar
 * Qualified Name:     net.minecraft.server.BiomeBase
 * JD-Core Version:    0.6.0
 */