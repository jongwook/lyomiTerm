//
//  lyomiTermView.h
//  lyomiTerm
//
//

#import <Cocoa/Cocoa.h>
#include "PieConnection.h"

@interface PieView : NSView {
	
	PieConnection *pie;
	NSFont *font;
	CGColorRef defaultForeground, defaultBackground, cursorColor;
	CGColorRef colors[8];
	NSColor *nsColors[8], *nsForeground, *nsBackground;
	BOOL init;
	unichar tempCharacter;
}

- (BOOL)acceptsFirstResponder;
- (void)keyDown:(NSEvent *)theEvent;
- (void)sendKey:(int)key;
- (void)sendString:(NSString *)str;
- (void)connected;
- (void)disconnected;
- (void)restoreTitle;
- (void)splashStatus:(NSString *)status;

@property (nonatomic,retain) PieConnection *pie;

@end
