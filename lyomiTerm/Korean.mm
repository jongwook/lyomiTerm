/*
 *  Korean.mm
 *  Pie
 *
 *  Created by Jong Wook Kim on 2/13/11.
 *  Copyright 2011 jongwook. All rights reserved.
 *
 */

#include "Korean.h"

#define MIX(hi,lo)	(((hi)<<16)|(lo))

static const int c[19] = { 0x3131, 0x3132, 0x3134, 0x3137, 0x3138, 0x3139, 0x3141, 0x3142, 0x3143, 0x3145,
	0x3146, 0x3147, 0x3148, 0x3149, 0x314a, 0x314b, 0x314c, 0x314d, 0x314e };
static const int v[21] = { 0x314f, 0x3150, 0x3151, 0x3152, 0x3153, 0x3154, 0x3155, 0x3156, 0x3157, 0x3158, 
	0x3159, 0x315a, 0x315b, 0x315c, 0x315d, 0x315e, 0x315f, 0x3160, 0x3161, 0x3162, 0x3163 };
static const int j[28] = { 0, 0x3131, 0x3132, 0x3133, 0x3134, 0x3135, 0x3136, 0x3137, 0x3139, 0x313a, 
				0x313b, 0x313c,	0x313d, 0x313e, 0x313f, 0x3140, 0x3141, 0x3142,	0x3144, 0x3145, 0x3146, 
				0x3147, 0x3148,	0x314a, 0x314b, 0x314c, 0x314d, 0x314e };

Korean::Korean() {
	size = 0;
	
	for(int i=0; i<19; ++i) {
		cho[c[i]] = i;
	}
	for(int i=0; i<21; ++i) {
		vowel[v[i]] = i;
	}
	for(int i=0; i<28; ++i) {
		jong[j[i]] = i;
	}
	
	complex[MIX(0x3131,0x3145)] = 0x3133;	// ㄳ
	complex[MIX(0x3134,0x3148)] = 0x3135;	// ㄵ
	complex[MIX(0x3134,0x314E)] = 0x3136;	// ㄶ
	complex[MIX(0x3139,0x3131)] = 0x313A;	// ㄺ
	complex[MIX(0x3139,0x3141)] = 0x313B;	// ㄻ
	complex[MIX(0x3139,0x3142)] = 0x313C;	// ㄼ
	complex[MIX(0x3139,0x3145)] = 0x313D;	// ㄽ
	complex[MIX(0x3139,0x314C)] = 0x313E;	// ㄾ
	complex[MIX(0x3139,0x314D)] = 0x313F;	// ㄿ
	complex[MIX(0x3139,0x314E)] = 0x3140;	// ㅀ
	complex[MIX(0x3142,0x3145)] = 0x3144;	// ㅄ

	complex[MIX(0x3157,0x314F)] = 0x3158;	// ㅘ 
	complex[MIX(0x3157,0x3150)] = 0x3159;	// ㅙ
	complex[MIX(0x3157,0x3163)] = 0x315A;	// ㅚ
	complex[MIX(0x315C,0x3153)] = 0x315D;	// ㅝ
	complex[MIX(0x315C,0x3154)] = 0x315E;	// ㅞ
	complex[MIX(0x315C,0x3164)] = 0x315F;	// ㅟ
	complex[MIX(0x3161,0x3163)] = 0x3162;	// ㅢ
	
}

unichar Korean::commit(int pos, int next) {
	if(next != -1) state = next;
	
	unichar result = value(pos);
	
	for(int i=pos; i<size; ++i) {
		buffer[i-pos] = buffer[i];
	}
	
	size -= pos;
	return result;
}

unichar Korean::add(unichar c) {
	// disregard other characters
	if(cho.count(c)==0 && vowel.count(c)==0) return 0;
	
	buffer[size++] = c;
	
	bool consonant = (vowel.count(c)==0);
	bool Vc = !consonant && (size >= 2) && (complex.count(MIX(buffer[size-2],buffer[size-1])) != 0);
	bool Tc = consonant	&& (size >= 4) && (complex.count(MIX(buffer[size-2],buffer[size-1])) != 0);
	bool L = ( c == 0x3138 ) || ( c == 0x3143 ) || ( c == 0x3149 );

	switch(state) {
		case 0:
			if(consonant)
				state = 1;
			else
				state = 2;
			break;
		case 1:
			if(consonant)
				return commit(1, 1);
			else 
				state = 3;
			break;
		case 2:
			if(consonant)
				return commit(1, 1);
			else if(Vc)
				state = 2;
			else
				return commit(1, 2);
			break;
		case 3:
			if(L)
				return commit(2, 1);
			else if(consonant)
				state = 5;
			else if(Vc)
				state = 6;
			else
				return commit(2, 2);
			break;
		case 4:
			if(consonant)
				return commit(2, 1);
			else
				return commit(2, 2);
			break;
		case 5:
			if(Tc)
				state = 7;
			else if(consonant)
				return commit(3, 1);
			else
				return commit(2, 3);
			break;
		case 6:
			if(L)
				return commit(3, 1);
			else if(consonant)
				state = 8;
			else
				return commit(3, 2);
			break;
		case 7:
			if(consonant)
				return commit(4, 1);
			else
				return commit(3, 3);
			break;
		case 8:
			if(Tc)
				state = 9;
			else if(consonant)
				return commit(4, 1);
			else
				return commit(3, 3);
			break;
		case 9:
			if(consonant)
				return commit(5, 1);
			else
				return commit(4, 3);
			break;
	}
	
	return 0;
}

unichar Korean::value(int pos) {
	if(pos == -1) pos = size;
	
	if(pos == 0) return 0;
	
	int a = 0, b = 0, c = 0;

	// only one to return
	if( pos == 1 ) {
		return buffer[0];
	}
	
	// complex vowel only 
	if(vowel.count(buffer[0]) != 0) {
		return complex[MIX(buffer[0],buffer[1])];
	}
	
	// choseong
	a = cho[buffer[0]];
	
	// jungseong
	b = vowel[buffer[1]];
	if(pos == 2) return 0xAC00 + a*21*28 + b*28;
	
	int i = 2;
	if(vowel.count(buffer[i]) != 0) {
		b = vowel[complex[MIX(buffer[1],buffer[i])]];
		i++;
	}
	
	if(pos == i) return 0xAC00 + a*21*28 + b*28;
	
	// jongseong
	c = jong[buffer[i++]];
	
	if (pos == i) return 0xAC00 + a*21*28 + b*28 + c;
	
	c = jong[complex[MIX(buffer[i-1],buffer[i])]];
	
	return 0xAC00 + a*21*28 + b*28 + c;
}

unichar Korean::clear() {
	unichar result = value();
	size = 0;
	state = 0;
	return result;
}

