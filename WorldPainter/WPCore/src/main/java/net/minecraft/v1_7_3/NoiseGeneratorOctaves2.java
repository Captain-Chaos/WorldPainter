/*    */ package net.minecraft.v1_7_3;
/*    */ 
/*    */ import java.util.Random;
/*    */ 
/*    */ public class NoiseGeneratorOctaves2 extends NoiseGenerator
/*    */ {
/*    */   private NoiseGenerator2[] a;
/*    */   private int b;
/*    */ 
/*    */   public NoiseGeneratorOctaves2(Random paramRandom, int paramInt)
/*    */   {
/* 14 */     this.b = paramInt;
/* 15 */     this.a = new NoiseGenerator2[paramInt];
/* 16 */     for (int i = 0; i < paramInt; i++)
/* 17 */       this.a[i] = new NoiseGenerator2(paramRandom);
/*    */   }
/*    */ 
/*    */   public double[] a(double[] paramArrayOfDouble, double paramDouble1, double paramDouble2, int paramInt1, int paramInt2, double paramDouble3, double paramDouble4, double paramDouble5)
/*    */   {
/* 46 */     return a(paramArrayOfDouble, paramDouble1, paramDouble2, paramInt1, paramInt2, paramDouble3, paramDouble4, paramDouble5, 0.5D);
/*    */   }
/*    */ 
/*    */   public double[] a(double[] paramArrayOfDouble, double paramDouble1, double paramDouble2, int paramInt1, int paramInt2, double paramDouble3, double paramDouble4, double paramDouble5, double paramDouble6) {
/* 50 */     paramDouble3 /= 1.5D;
/* 51 */     paramDouble4 /= 1.5D;
/*    */ 
/* 53 */     if ((paramArrayOfDouble == null) || (paramArrayOfDouble.length < paramInt1 * paramInt2)) paramArrayOfDouble = new double[paramInt1 * paramInt2]; else {
/* 54 */       for (int i = 0; i < paramArrayOfDouble.length; i++) {
/* 55 */         paramArrayOfDouble[i] = 0.0D;
/*    */       }
/*    */     }
/* 58 */     double d1 = 1.0D;
/* 59 */     double d2 = 1.0D;
/* 60 */     for (int j = 0; j < this.b; j++) {
/* 61 */       this.a[j].a(paramArrayOfDouble, paramDouble1, paramDouble2, paramInt1, paramInt2, paramDouble3 * d2, paramDouble4 * d2, 0.55D / d1);
/* 62 */       d2 *= paramDouble5;
/* 63 */       d1 *= paramDouble6;
/*    */     }
/*    */ 
/* 66 */     return paramArrayOfDouble;
/*    */   }
/*    */ }

/* Location:           /home/pepijn/.m2/repository/org/bukkit/minecraft-server/1.6.6/minecraft-server-1.6.6.jar
 * Qualified Name:     net.minecraft.server.NoiseGeneratorOctaves2
 * JD-Core Version:    0.6.0
 */