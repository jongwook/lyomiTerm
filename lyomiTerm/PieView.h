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
}

- (BOOL)acceptsFirstResponder;
- (void)keyDown:(NSEvent *)theEvent;

@property (nonatomic,retain) PieConnection *pie;

@end
