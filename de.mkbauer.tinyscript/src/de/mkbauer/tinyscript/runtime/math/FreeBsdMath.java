/*
 * Ports of log2 (e_log2.c, k_log.h) and cbrt (s_cbrt.c) from FreeBSD's msun, the versions V8
 * (and so node) uses, so that Math.log2 and Math.cbrt agree with node to the last bit (checked
 * on 20,000 inputs). Java's StrictMath has no log2 and an older cbrt.
 *
 * ====================================================
 * Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.
 *
 * Developed at SunPro, a Sun Microsystems, Inc. business.
 * Permission to use, copy, modify, and distribute this
 * software is freely granted, provided that this notice
 * is preserved.
 * ====================================================
 *
 * cbrt optimized by Bruce D. Evans.
 */
package de.mkbauer.tinyscript.runtime.math;

final class FreeBsdMath {

	private FreeBsdMath() {
	}

	private static int high(double x) {
		return (int) (Double.doubleToRawLongBits(x) >>> 32);
	}

	private static int low(double x) {
		return (int) Double.doubleToRawLongBits(x);
	}

	private static double words(int high, int low) {
		return Double.longBitsToDouble(((long) high << 32) | (low & 0xffffffffL));
	}

	// log2 (e_log2.c with k_log1p from k_log.h)

	private static final double TWO54 = 1.80143985094819840000e+16;
	private static final double IVLN2HI = 1.44269504072144627571e+00;
	private static final double IVLN2LO = 1.67517131648865118353e-10;
	private static final double LG1 = 6.666666666666735130e-01;
	private static final double LG2 = 3.999999999940941908e-01;
	private static final double LG3 = 2.857142874366239149e-01;
	private static final double LG4 = 2.222219843214978396e-01;
	private static final double LG5 = 1.818357216161805012e-01;
	private static final double LG6 = 1.531383769920937332e-01;
	private static final double LG7 = 1.479819860511658591e-01;

	private static double kLog1p(double f) {
		double s = f / (2.0 + f);
		double z = s * s;
		double w = z * z;
		double t1 = w * (LG2 + w * (LG4 + w * LG6));
		double t2 = z * (LG1 + w * (LG3 + w * (LG5 + w * LG7)));
		double r = t2 + t1;
		double hfsq = 0.5 * f * f;
		return s * (hfsq + r);
	}

	static double log2(double x) {
		int hx = high(x);
		int lx = low(x);
		int k = 0;
		if (hx < 0x00100000) {
			if (((hx & 0x7fffffff) | lx) == 0)
				return Double.NEGATIVE_INFINITY;   // log(+-0) = -inf
			if (hx < 0)
				return Double.NaN;                 // log(-x) = NaN
			k -= 54;
			x *= TWO54;                            // subnormal: scale up
			hx = high(x);
		}
		if (hx >= 0x7ff00000)
			return x + x;
		if (hx == 0x3ff00000 && lx == 0)
			return 0;                              // log(1) = +0
		k += (hx >> 20) - 1023;
		hx &= 0x000fffff;
		int i = (hx + 0x95f64) & 0x100000;
		x = words(hx | (i ^ 0x3ff00000), low(x));  // normalize x or x/2
		k += (i >> 20);
		double y = k;
		double f = x - 1.0;
		double hfsq = 0.5 * f * f;
		double r = kLog1p(f);
		double hi = words(high(f - hfsq), 0);
		double lo = (f - hi) - hfsq + r;
		double valHi = hi * IVLN2HI;
		double valLo = (lo + hi) * IVLN2LO + lo * IVLN2HI;
		double w = y + valHi;
		valLo += (y - w) + valHi;
		valHi = w;
		return valLo + valHi;
	}

	// cbrt (s_cbrt.c)

	private static final int B1 = 715094163;   // (1023 - 1023 / 3 - 0.03306235651) * 2^20
	private static final int B2 = 696219795;   // (1023 - 1023 / 3 - 54 / 3 - 0.03306235651) * 2^20
	private static final double P0 = 1.87595182427177009643;
	private static final double P1 = -1.88497979543377169875;
	private static final double P2 = 1.621429720105354466140;
	private static final double P3 = -0.758397934778766047437;
	private static final double P4 = 0.145996192886612446982;

	static double cbrt(double x) {
		int hx = high(x);
		int lx = low(x);
		int sign = hx & 0x80000000;
		hx ^= sign;
		if (Integer.compareUnsigned(hx, 0x7ff00000) >= 0)
			return x + x;                          // cbrt(NaN, Infinity) is itself
		double t;
		// Rough cbrt to 5 bits
		if (Integer.compareUnsigned(hx, 0x00100000) < 0) {
			if ((hx | lx) == 0)
				return x;                          // cbrt(+-0) is itself
			t = words(0x43500000, 0) * x;          // 2^54 * x
			t = words(sign | ((high(t) & 0x7fffffff) / 3 + B2), 0);
		}
		else
			t = words(sign | (hx / 3 + B1), 0);
		// New cbrt to 23 bits
		double r = (t * t) * (t / x);
		t = t * ((P0 + r * (P1 + r * P2)) + ((r * r) * r) * (P3 + r * P4));
		// Round t away from zero to 23 bits
		t = Double.longBitsToDouble((Double.doubleToRawLongBits(t) + 0x80000000L) & 0xffffffffc0000000L);
		// One Newton step to 53 bits with an error < 0.667 ulps
		double s = t * t;
		r = x / s;
		double w = t + t;
		r = (r - t) / (w + r);
		return t + t * r;
	}
}
